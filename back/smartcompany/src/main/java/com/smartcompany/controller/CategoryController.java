package com.smartcompany.controller;

import com.smartcompany.dto.product.CategoryDto;
import com.smartcompany.dto.product.CategoryRequestDto;
import com.smartcompany.service.product.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/categories")
@Tag(name = "Categories", description = "Upravljanje kategorijama proizvoda")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Operation(summary = "Sve kategorije", description = "Javni endpoint (per SecurityConfig)")
    @SecurityRequirements
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CategoryDto>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @Operation(summary = "Kategorija po ID-u", description = "Javni endpoint (per SecurityConfig)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Kategorija pronađena"),
        @ApiResponse(responseCode = "404", description = "Kategorija nije pronađena")
    })
    @SecurityRequirements
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @Operation(summary = "Kreiranje kategorije", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Kategorija kreirana"),
        @ApiResponse(responseCode = "400", description = "Kategorija sa tim nazivom već postoji"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryDto> create(@RequestBody @Valid CategoryRequestDto dto) {
        CategoryDto created = categoryService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Izmena kategorije", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Kategorija izmenjena"),
        @ApiResponse(responseCode = "404", description = "Kategorija nije pronađena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryDto> update(@PathVariable Long id, @RequestBody @Valid CategoryRequestDto dto) {
        return ResponseEntity.ok(categoryService.update(id, dto));
    }

    @Operation(summary = "Brisanje kategorije", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Kategorija obrisana"),
        @ApiResponse(responseCode = "404", description = "Kategorija nije pronađena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}