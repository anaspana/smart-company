package com.smartcompany.service.product;

import com.smartcompany.dto.cache.CachedPage;
import com.smartcompany.dto.factory.FactorySummaryDto;
import com.smartcompany.dto.product.CategoryDto;
import com.smartcompany.dto.product.ProductDto;
import com.smartcompany.dto.product.ProductRequestDto;
import com.smartcompany.exception.AppException;
import com.smartcompany.model.product.Category;
import com.smartcompany.model.product.Product;
import com.smartcompany.repository.factory.FactoryRepository;
import com.smartcompany.repository.factory.ProductionRecordRepository;
import com.smartcompany.repository.order.OrderItemRepository;
import com.smartcompany.repository.product.CategoryRepository;
import com.smartcompany.repository.product.ProductRepository;
import com.smartcompany.repository.warehouse.WarehouseItemRepository;
import com.smartcompany.specification.ProductSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FactoryRepository factoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private WarehouseItemRepository warehouseItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductionRecordRepository productionRecordRepository;

    @Autowired
    private CacheManager cacheManager;

    public Page<ProductDto> search(String search, Long categoryId, Boolean isForSale,
                                BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Cache cache = cacheManager.getCache("products");
        String key = search + "-" + categoryId + "-" + isForSale + "-" + minPrice + "-" + maxPrice
                + "-" + pageable.getPageNumber() + "-" + pageable.getPageSize() + "-" + pageable.getSort();

        if (cache != null) {
            CachedPage<ProductDto> cached = cache.get(key, CachedPage.class);
            if (cached != null) {
                return cached.toPage(pageable);
            }
        }

        Specification<Product> spec = ProductSpecification.search(search, categoryId, isForSale, minPrice, maxPrice);
        Page<ProductDto> result = productRepository.findAll(spec, pageable).map(this::toDto);

        if (cache != null) {
            cache.put(key, CachedPage.from(result));
        }

        return result;
    }

    @Cacheable(value = "product", key = "#id")
    public ProductDto getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));
        return toDtoWithFactories(product);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDto create(ProductRequestDto dto) {
        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new AppException("Category not found", HttpStatus.NOT_FOUND));

        Product product = new Product();
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        product.setWeight(dto.weight());
        product.setImage(dto.image());
        product.setForSale(dto.isForSale());
        product.setCategory(category);

        Product saved = productRepository.save(product);

        return toDto(saved);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "product", key = "#id")
    })
    public ProductDto update(Long id, ProductRequestDto dto) {
        Product product;
        try {
            product = productRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));
        } catch (PessimisticLockingFailureException e) {
            throw new AppException(
                "Proizvod se trenutno menja od strane drugog menadžera, pokušajte ponovo",
                HttpStatus.CONFLICT
            );
        }

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new AppException("Category not found", HttpStatus.NOT_FOUND));

        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        product.setWeight(dto.weight());
        product.setImage(dto.image());
        product.setForSale(dto.isForSale());
        product.setCategory(category);

        Product saved = productRepository.save(product);

        return toDto(saved);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "product", key = "#id")
    })
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new AppException("Product not found", HttpStatus.NOT_FOUND);
        }

        if (orderItemRepository.existsByProductId(id)) {
            throw new AppException("Cannot delete product with existing orders", HttpStatus.CONFLICT);
        }

        if (warehouseItemRepository.existsByProductId(id)) {
            throw new AppException("Cannot delete product that is stocked in a warehouse", HttpStatus.CONFLICT);
        }

        if (productionRecordRepository.existsByProductId(id)) {
            throw new AppException("Cannot delete product with existing production records", HttpStatus.CONFLICT);
        }

        factoryRepository.deleteProductAssociations(id);
        productRepository.deleteById(id);
    }

    public ProductDto toDto(Product product) {
        CategoryDto categoryDto = categoryService.toDto(product.getCategory());
        Integer totalStock = warehouseItemRepository.sumQuantityByProductId(product.getId());

        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getWeight(),
                product.getImage(),
                product.isForSale(),
                categoryDto,
                totalStock,
                null
        );
    }

    public ProductDto toDtoWithFactories(Product product) {
        ProductDto base = toDto(product);
        List<FactorySummaryDto> factories = factoryRepository.findByProductId(product.getId()).stream()
                .map(f -> new FactorySummaryDto(f.getId(), f.getName()))
                .toList();
        base.setFactories(factories);
        return base;
    }

}