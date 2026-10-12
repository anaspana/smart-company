package com.smartcompany.controller;

import com.smartcompany.dto.company.CompanyDto;
import com.smartcompany.dto.order.CreateOrderDto;
import com.smartcompany.dto.order.OrderResponseDto;
import com.smartcompany.dto.product.CategoryDto;
import com.smartcompany.dto.product.ProductDto;
import com.smartcompany.dto.user.UserDto;
import com.smartcompany.service.order.OrderService;
import com.smartcompany.service.product.CategoryService;
import com.smartcompany.service.product.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Kreiranje porudžbina i pomoćni podaci za formu porudžbine")
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;
    private final CategoryService categoryService;

    @Operation(summary = "Moje firme", description = "Vraća odobrene firme prijavljenog kupca")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista firmi vraćena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @GetMapping("/my-companies")
    @PreAuthorize("hasRole('Customer')")
    public ResponseEntity<List<CompanyDto>> getMyCompanies(@AuthenticationPrincipal UserDto user) {
        return ResponseEntity.ok(orderService.getMyCompanies(user.getUsername()));
    }

    @Operation(summary = "Pretraga proizvoda za porudžbinu", description = "Vraća stranicu proizvoda koji su na prodaji")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stranica proizvoda vraćena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @GetMapping("/products")
    @PreAuthorize("hasRole('Customer')")
    public ResponseEntity<Page<ProductDto>> getProducts(
            @Parameter(description = "Pretraga po nazivu proizvoda") @RequestParam(required = false) String name,
            @Parameter(description = "ID kategorije") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Broj stranice") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Veličina stranice") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Polje za sortiranje") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Smer sortiranja") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Minimalna cena") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maksimalna cena") @RequestParam(required = false) BigDecimal maxPrice) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        var pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(productService.search(name, categoryId, true, minPrice, maxPrice, pageable));
    }

    @Operation(summary = "Sve kategorije", description = "Za filter u formi porudžbine")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista kategorija vraćena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @GetMapping("/categories")
    @PreAuthorize("hasRole('Customer')")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @Operation(summary = "Kreiranje porudžbine", description = "Smanjuje zalihe u magacinima i šalje PDF fakturu na email kupca")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Porudžbina uspešno kreirana"),
        @ApiResponse(responseCode = "400", description = "Nevalidan zahtev"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup"),
        @ApiResponse(responseCode = "403", description = "Firma ne pripada prijavljenom korisniku ili nije odobrena"),
        @ApiResponse(responseCode = "404", description = "Firma ili proizvod nisu pronađeni"),
        @ApiResponse(responseCode = "409", description = "Nedovoljno zaliha ili sistem obrađuje konkurentnu porudžbinu za isti proizvod")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('Customer')")
    public ResponseEntity<OrderResponseDto> createOrder(
            @AuthenticationPrincipal UserDto user,
            @RequestBody @Valid CreateOrderDto dto) {
        return ResponseEntity.ok(orderService.createOrder(user.getUsername(), dto));
    }
}