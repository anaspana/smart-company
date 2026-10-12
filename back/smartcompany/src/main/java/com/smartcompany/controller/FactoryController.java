package com.smartcompany.controller;

import com.smartcompany.dto.factory.FactoryDto;
import com.smartcompany.dto.factory.FactoryRequestDto;
import com.smartcompany.dto.factory.ProductionDataDto;
import com.smartcompany.dto.location.CityDto;
import com.smartcompany.dto.location.CountryDto;
import com.smartcompany.service.factory.FactoryService;
import com.smartcompany.service.factory.ProductionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/factories")
@Tag(name = "Factories", description = "Upravljanje fabrikama i pregled proizvodnje")
@SecurityRequirement(name = "bearerAuth")
public class FactoryController {

    @Autowired private FactoryService factoryService;
    @Autowired private ProductionService productionService;

    @Operation(summary = "Pretraga fabrika")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stranica fabrika vraćena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<Page<FactoryDto>> search(
            @Parameter(description = "Pretraga po nazivu/adresi") @RequestParam(required = false) String search,
            @Parameter(description = "ID države") @RequestParam(required = false) Long countryId,
            @Parameter(description = "ID grada") @RequestParam(required = false) Long cityId,
            @Parameter(description = "Filter po online statusu") @RequestParam(required = false) Boolean isOnline,
            @Parameter(description = "Broj stranice") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Veličina stranice") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Polje za sortiranje") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Smer sortiranja") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return ResponseEntity.ok(
            factoryService.search(search, countryId, cityId, isOnline, PageRequest.of(page, size, sort))
        );
    }

    @Operation(summary = "Detalji fabrike po ID-u")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fabrika pronađena"),
        @ApiResponse(responseCode = "404", description = "Fabrika nije pronađena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<FactoryDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(factoryService.getById(id));
    }

    @Operation(summary = "Kreiranje fabrike", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Fabrika kreirana"),
        @ApiResponse(responseCode = "400", description = "Nevalidan zahtev"),
        @ApiResponse(responseCode = "404", description = "Država ili grad nisu pronađeni"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<FactoryDto> create(@RequestBody @Valid FactoryRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(factoryService.create(dto));
    }

    @Operation(summary = "Izmena fabrike", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fabrika izmenjena"),
        @ApiResponse(responseCode = "400", description = "Nevalidan zahtev"),
        @ApiResponse(responseCode = "404", description = "Fabrika, država ili grad nisu pronađeni"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<FactoryDto> update(@PathVariable Long id, @RequestBody @Valid FactoryRequestDto dto) {
        return ResponseEntity.ok(factoryService.update(id, dto));
    }

    @Operation(summary = "Brisanje fabrike", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Fabrika obrisana"),
        @ApiResponse(responseCode = "404", description = "Fabrika nije pronađena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        factoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Sve države")
    @GetMapping(value = "/countries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CountryDto>> getCountries() {
        return ResponseEntity.ok(factoryService.getAllCountries());
    }

    @Operation(summary = "Gradovi po državi")
    @GetMapping(value = "/cities", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CityDto>> getCities(@Parameter(description = "ID države") @RequestParam Long countryId) {
        return ResponseEntity.ok(factoryService.getCitiesByCountry(countryId));
    }

    @Operation(
        summary = "Podaci proizvodnje za grafik (5.10)",
        description = "Period: WEEK | MONTH | THREE_MONTHS | SIX_MONTHS | YEAR | CUSTOM. Za CUSTOM prosledi from/to kao yyyy-MM-dd. Maksimalno 365 dana."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Podaci vraćeni"),
        @ApiResponse(responseCode = "400", description = "Nevalidan period ili opseg datuma")
    })
    @GetMapping(value = "/{factoryId}/products/{productId}/production", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<ProductionDataDto> getProductionData(
            @PathVariable Long factoryId,
            @PathVariable Long productId,
            @Parameter(description = "WEEK | MONTH | THREE_MONTHS | SIX_MONTHS | YEAR | CUSTOM") @RequestParam(defaultValue = "WEEK") String period,
            @Parameter(description = "Početni datum za CUSTOM (yyyy-MM-dd)") @RequestParam(required = false) String from,
            @Parameter(description = "Krajnji datum za CUSTOM (yyyy-MM-dd)") @RequestParam(required = false) String to
    ) {
        return ResponseEntity.ok(productionService.getProductionData(factoryId, productId, period, from, to));
    }
}