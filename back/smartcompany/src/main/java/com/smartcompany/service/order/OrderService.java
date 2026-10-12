package com.smartcompany.service.order;

import com.smartcompany.dto.company.CompanyDto;
import com.smartcompany.dto.order.CreateOrderDto;
import com.smartcompany.dto.order.OrderResponseDto;
import com.smartcompany.exception.AppException;
import com.smartcompany.model.company.Company;
import com.smartcompany.model.company.CompanyStatus;
import com.smartcompany.model.order.Order;
import com.smartcompany.model.order.OrderItem;
import com.smartcompany.model.product.Product;
import com.smartcompany.model.user.User;
import com.smartcompany.model.warehouse.WarehouseItem;
import com.smartcompany.repository.company.CompanyRepository;
import com.smartcompany.repository.order.OrderRepository;
import com.smartcompany.repository.product.ProductRepository;
import com.smartcompany.repository.user.UserRepository;
import com.smartcompany.repository.warehouse.WarehouseItemRepository;
import com.smartcompany.service.user.EmailService;
import com.smartcompany.service.user.PdfService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderService {

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    private final OrderRepository orderRepository;
    private final WarehouseItemRepository warehouseItemRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final OrderService self; // self-injection, za pravilan @Transactional proxy poziv

    public OrderService(
            OrderRepository orderRepository,
            WarehouseItemRepository warehouseItemRepository,
            CompanyRepository companyRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            PdfService pdfService,
            EmailService emailService,
            @Lazy OrderService self) {
        this.orderRepository = orderRepository;
        this.warehouseItemRepository = warehouseItemRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.self = self;
    }

    public List<CompanyDto> getMyCompanies(String username) {
        return companyRepository
            .findByOwnerUsernameAndStatus(username, CompanyStatus.APPROVED)
            .stream()
            .map(c -> new CompanyDto(
                c.getId(),
                c.getName(),
                c.getCity().getName(),
                c.getCountry().getName()))
            .toList();
    }

    /**
     * Javni ulaz — NIJE @Transactional. Deleguje kritični, zaključavajući deo
     * u self.createOrderTransactional() (kratka transakcija), a PDF generisanje
     * i slanje email-a rade se OVDE, van transakcije, da se pesimistički
     * lock nad WarehouseItem redovima ne drži tokom sporog I/O (SMTP, PDF render).
     */
    public OrderResponseDto createOrder(String customerUsername, CreateOrderDto dto) {
        Order saved = self.createOrderTransactional(customerUsername, dto);

        try {
            self.generateAndSendInvoiceAsync(saved.getId());
        } catch (Exception e) {
            System.err.println("[OrderService] Could not submit invoice task for order " + saved.getId() + ": " + e.getMessage());
        }

        return toDto(saved);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "product", allEntries = true)
    })
    public Order createOrderTransactional(String customerUsername, CreateOrderDto dto) {
        User customer = userRepository.findById(customerUsername)
            .orElseThrow(() -> new AppException("Korisnik nije pronađen", HttpStatus.NOT_FOUND));

        Company company = companyRepository.findById(dto.companyId())
            .orElseThrow(() -> new AppException("Firma nije pronađena", HttpStatus.NOT_FOUND));

        if (!company.getOwner().getUsername().equals(customerUsername)) {
            throw new AppException("Firma ne pripada ovom korisniku", HttpStatus.FORBIDDEN);
        }

        if (company.getStatus() != CompanyStatus.APPROVED) {
            throw new AppException("Firma nije odobrena", HttpStatus.BAD_REQUEST);
        }

        List<CreateOrderDto.OrderItemDto> sortedItems = dto.items().stream()
            .sorted(Comparator.comparing(CreateOrderDto.OrderItemDto::productId))
            .toList();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        try {
            for (CreateOrderDto.OrderItemDto itemDto : sortedItems) {
                Product product = productRepository.findById(itemDto.productId())
                    .orElseThrow(() -> new AppException(
                        "Proizvod nije pronađen: " + itemDto.productId(), HttpStatus.NOT_FOUND));

                if (!product.isForSale()) {
                    throw new AppException("Proizvod nije na prodaji: " + product.getName(),
                        HttpStatus.BAD_REQUEST);
                }

                List<WarehouseItem> toDeduct;

                // Pokušaj prvo da nađeš JEDAN magacin koji sam pokriva celu količinu —
                // zaključava samo taj jedan red, ne sve magacine sa tim proizvodom.
                List<WarehouseItem> singleSufficient =
                    warehouseItemRepository.findSingleWarehouseWithSufficientStock(product.getId(), itemDto.quantity());

                if (!singleSufficient.isEmpty()) {
                    toDeduct = List.of(singleSufficient.get(0));
                } else {
                    // Nijedan pojedinačni magacin nema dovoljno — padni na skupljanje iz više,
                    // isti pristup kao pre (zaključava sve redove za taj proizvod).
                    List<WarehouseItem> warehouseItems =
                        warehouseItemRepository.findByProductIdWithStock(product.getId());

                    int totalAvailable = warehouseItems.stream()
                        .mapToInt(WarehouseItem::getQuantity).sum();

                    if (totalAvailable < itemDto.quantity()) {
                        throw new AppException(
                            "Nedovoljno zaliha za: " + product.getName() +
                            " (traženo: " + itemDto.quantity() + ", dostupno: " + totalAvailable + ")",
                            HttpStatus.CONFLICT
                        );
                    }

                    toDeduct = new ArrayList<>(warehouseItems);
                    toDeduct.sort(Comparator.comparing(WarehouseItem::getQuantity).reversed());
                }

                int remaining = itemDto.quantity();
                for (WarehouseItem wi : toDeduct) {
                    if (remaining <= 0) break;
                    int deduct = Math.min(wi.getQuantity(), remaining);
                    wi.setQuantity(wi.getQuantity() - deduct);
                    warehouseItemRepository.save(wi);
                    remaining -= deduct;
                }

                OrderItem oi = new OrderItem();
                oi.setProduct(product);
                oi.setQuantity(itemDto.quantity());
                oi.setPriceAtOrder(product.getPrice());
                orderItems.add(oi);

                total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemDto.quantity())));
            }
        } catch (PessimisticLockingFailureException e) {
            throw new AppException(
                "Sistem je trenutno zauzet obradom porudžbina za ove proizvode, pokušajte ponovo",
                HttpStatus.CONFLICT
            );
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setCompany(company);
        order.setCreatedAt(Instant.now());
        order.setTotalPrice(total);
        order.setItems(orderItems);
        orderItems.forEach(oi -> oi.setOrder(order));

        return orderRepository.save(order);
    }

    /**
     * Radi u posebnom threadu, van glavnog request threada — korisnik dobija
     * odgovor odmah nakon commit-a porudžbine, ne čeka PDF/email.
     * @Transactional(readOnly = true) učitava order + lazy relacije (customer, items)
     * u SVOM transaction/session kontekstu, jer se izvršava nezavisno od originalnog zahteva.
     */
    @Async("invoiceExecutor")
    @Transactional(readOnly = true)
    public void generateAndSendInvoiceAsync(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new AppException("Order not found", HttpStatus.NOT_FOUND));

        try {
            byte[] pdf = pdfService.generateInvoice(order);

            if (emailEnabled) {
                emailService.sendInvoice(order.getCustomer().getEmail(), pdf, order.getId());
            } 
            // else {
            //     System.out.println("[OrderService] Email disabled (load testing mode) - PDF generated but not sent for order " + orderId);
            // }
        } catch (Exception e) {
            System.err.println("[OrderService] Failed to generate/send invoice for order " + orderId + ": " + e.getMessage());
        }
    }

    private OrderResponseDto toDto(Order order) {
        return new OrderResponseDto(
            order.getId(),
            order.getCreatedAt().toString(),
            order.getTotalPrice(),
            order.getCompany().getId(),
            order.getCompany().getName(),
            order.getItems().stream().map(oi -> new OrderResponseDto.OrderItemResponseDto(
                oi.getProduct().getId(),
                oi.getProduct().getName(),
                oi.getQuantity(),
                oi.getPriceAtOrder()
            )).toList()
        );
    }
}