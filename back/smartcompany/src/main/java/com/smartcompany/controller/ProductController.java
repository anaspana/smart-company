package com.smartcompany.controller;

import com.smartcompany.dto.product.ProductDto;
import com.smartcompany.dto.product.ProductRequestDto;
import com.smartcompany.service.product.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/products")
@Tag(name = "Products", description = "Upravljanje proizvodima i pretraga")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Operation(
        summary = "Pretraga i filtriranje proizvoda",
        description = "Javni endpoint (per SecurityConfig). Pretraga po nazivu/opisu, filter po kategoriji, dostupnosti, opsegu cene, sa paginacijom i sortiranjem."
    )
    @SecurityRequirements
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ProductDto>> search(
            @Parameter(description = "Pretraga po nazivu/opisu") @RequestParam(required = false) String search,
            @Parameter(description = "Filter po kategoriji") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter po dostupnosti") @RequestParam(required = false) Boolean isForSale,
            @Parameter(description = "Minimalna cena") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maksimalna cena") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Broj stranice") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Veličina stranice") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Polje za sortiranje (name, price, weight)") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Smer sortiranja") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(productService.search(search, categoryId, isForSale, minPrice, maxPrice, pageable));
    }

    @Operation(summary = "Detalji proizvoda po ID-u", description = "Javni endpoint (per SecurityConfig)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Proizvod pronađen"),
        @ApiResponse(responseCode = "404", description = "Proizvod nije pronađen")
    })
    @SecurityRequirements
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @Operation(summary = "Kreiranje proizvoda", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Proizvod kreiran"),
        @ApiResponse(responseCode = "400", description = "Nevalidan zahtev"),
        @ApiResponse(responseCode = "404", description = "Kategorija nije pronađena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductDto> create(@RequestBody @Valid ProductRequestDto dto) {
        ProductDto created = productService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Izmena proizvoda", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Proizvod izmenjen"),
        @ApiResponse(responseCode = "404", description = "Proizvod ili kategorija nisu pronađeni"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup"),
        @ApiResponse(responseCode = "409", description = "Proizvod trenutno menja drugi menadžer, pokušajte ponovo")
    })
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductDto> update(@PathVariable Long id, @RequestBody @Valid ProductRequestDto dto) {
        return ResponseEntity.ok(productService.update(id, dto));
    }

    @Operation(summary = "Brisanje proizvoda", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Proizvod obrisan"),
        @ApiResponse(responseCode = "404", description = "Proizvod nije pronađen"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}