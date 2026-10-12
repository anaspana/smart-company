package com.smartcompany.controller;

import com.smartcompany.dto.location.CityDto;
import com.smartcompany.dto.location.CountryDto;
import com.smartcompany.dto.warehouse.TemperatureDataDto;
import com.smartcompany.dto.warehouse.WarehouseDto;
import com.smartcompany.dto.warehouse.WarehouseRequestDto;
import com.smartcompany.service.warehouse.TemperatureService;
import com.smartcompany.service.warehouse.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/warehouses")
@Tag(name = "Warehouses", description = "Upravljanje magacinima, sektorima i pregled temperature")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final com.smartcompany.service.factory.FactoryService factoryService;
    private final TemperatureService temperatureService;

    @Operation(summary = "Pretraga magacina")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stranica magacina vraćena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup"),
        @ApiResponse(responseCode = "403", description = "Nedovoljna prava pristupa")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<Page<WarehouseDto>> search(
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
            warehouseService.search(search, countryId, cityId, isOnline, PageRequest.of(page, size, sort))
        );
    }

    @Operation(summary = "Detalji magacina po ID-u")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Magacin pronađen"),
        @ApiResponse(responseCode = "404", description = "Magacin nije pronađen"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<WarehouseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getById(id));
    }

    @Operation(summary = "Kreiranje magacina", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Magacin kreiran"),
        @ApiResponse(responseCode = "400", description = "Nevalidan zahtev"),
        @ApiResponse(responseCode = "404", description = "Država ili grad nisu pronađeni"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<WarehouseDto> create(@RequestBody @Valid WarehouseRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.create(dto));
    }

    @Operation(summary = "Izmena magacina", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Magacin izmenjen"),
        @ApiResponse(responseCode = "400", description = "Nevalidan zahtev"),
        @ApiResponse(responseCode = "404", description = "Magacin, država ili grad nisu pronađeni"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<WarehouseDto> update(@PathVariable Long id, @RequestBody @Valid WarehouseRequestDto dto) {
        return ResponseEntity.ok(warehouseService.update(id, dto));
    }

    @Operation(summary = "Brisanje magacina", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Magacin obrisan"),
        @ApiResponse(responseCode = "404", description = "Magacin nije pronađen"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Sve države", description = "Reuse FactoryService")
    @GetMapping("/countries")
    public ResponseEntity<List<CountryDto>> getCountries() {
        return ResponseEntity.ok(factoryService.getAllCountries());
    }

    @Operation(summary = "Gradovi po državi")
    @GetMapping("/cities")
    public ResponseEntity<List<CityDto>> getCities(@Parameter(description = "ID države") @RequestParam Long countryId) {
        return ResponseEntity.ok(factoryService.getCitiesByCountry(countryId));
    }

    @Operation(
        summary = "Podaci temperature za grafik sektora (5.11)",
        description = "Period: WEEK | MONTH | THREE_MONTHS | SIX_MONTHS | YEAR | CUSTOM. Za CUSTOM prosledi from/to kao yyyy-MM-dd. Maksimalno 365 dana."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Podaci vraćeni"),
        @ApiResponse(responseCode = "400", description = "Nevalidan period ili opseg datuma"),
        @ApiResponse(responseCode = "500", description = "Greška pri čitanju iz InfluxDB")
    })
    @GetMapping("/{warehouseId}/sectors/{sectorId}/temperature")
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<TemperatureDataDto> getTemperatureData(
            @PathVariable Long warehouseId,
            @PathVariable Long sectorId,
            @Parameter(description = "WEEK | MONTH | THREE_MONTHS | SIX_MONTHS | YEAR | CUSTOM") @RequestParam(defaultValue = "WEEK") String period,
            @Parameter(description = "Početni datum za CUSTOM (yyyy-MM-dd)") @RequestParam(required = false) String from,
            @Parameter(description = "Krajnji datum za CUSTOM (yyyy-MM-dd)") @RequestParam(required = false) String to
    ) {
        return ResponseEntity.ok(temperatureService.getTemperatureData(warehouseId, sectorId, period, from, to));
    }
}