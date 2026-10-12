package com.smartcompany.controller;

import com.smartcompany.dto.vehicle.*;
import com.smartcompany.service.vehicle.DistanceService;
import com.smartcompany.service.vehicle.VehicleService;
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
@RequestMapping("/vehicles")
@Tag(name = "Vehicles", description = "Upravljanje dostavnim vozilima i pregled pređene distance")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final DistanceService distanceService;

    @Operation(summary = "Pretraga vozila")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stranica vozila vraćena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<Page<VehicleDto>> search(
            @Parameter(description = "Pretraga po registarskoj oznaci") @RequestParam(required = false) String search,
            @Parameter(description = "ID marke") @RequestParam(required = false) Long brandId,
            @Parameter(description = "ID modela") @RequestParam(required = false) Long modelId,
            @Parameter(description = "Filter po online statusu") @RequestParam(required = false) Boolean isOnline,
            @Parameter(description = "Broj stranice") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Veličina stranice") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Polje za sortiranje") @RequestParam(defaultValue = "licensePlate") String sortBy,
            @Parameter(description = "Smer sortiranja") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        return ResponseEntity.ok(
            vehicleService.search(search, brandId, modelId, isOnline, PageRequest.of(page, size, sort))
        );
    }

    @Operation(summary = "Detalji vozila po ID-u")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vozilo pronađeno"),
        @ApiResponse(responseCode = "404", description = "Vozilo nije pronađeno"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<VehicleDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getById(id));
    }

    @Operation(summary = "Kreiranje vozila", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Vozilo kreirano"),
        @ApiResponse(responseCode = "400", description = "Nevalidan zahtev ili model ne pripada marki"),
        @ApiResponse(responseCode = "404", description = "Marka ili model nisu pronađeni"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<VehicleDto> create(@RequestBody @Valid VehicleRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(dto));
    }

    @Operation(summary = "Izmena vozila", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vozilo izmenjeno"),
        @ApiResponse(responseCode = "400", description = "Nevalidan zahtev ili model ne pripada marki"),
        @ApiResponse(responseCode = "404", description = "Vozilo, marka ili model nisu pronađeni"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<VehicleDto> update(@PathVariable Long id, @RequestBody @Valid VehicleRequestDto dto) {
        return ResponseEntity.ok(vehicleService.update(id, dto));
    }

    @Operation(summary = "Brisanje vozila", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Vozilo obrisano"),
        @ApiResponse(responseCode = "404", description = "Vozilo nije pronađeno"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Sve marke vozila")
    @GetMapping("/brands")
    public ResponseEntity<List<VehicleBrandDto>> getBrands() {
        return ResponseEntity.ok(vehicleService.getAllBrands());
    }

    @Operation(summary = "Modeli po marki")
    @GetMapping("/models")
    public ResponseEntity<List<VehicleModelDto>> getModels(@Parameter(description = "ID marke") @RequestParam Long brandId) {
        return ResponseEntity.ok(vehicleService.getModelsByBrand(brandId));
    }

    @Operation(
        summary = "Podaci pređene distance za grafik (5.12)",
        description = "Period: WEEK | MONTH | THREE_MONTHS | SIX_MONTHS | YEAR | CUSTOM. Za CUSTOM prosledi from/to kao yyyy-MM-dd. Maksimalno 365 dana."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Podaci vraćeni"),
        @ApiResponse(responseCode = "400", description = "Nevalidan period ili opseg datuma"),
        @ApiResponse(responseCode = "500", description = "Greška pri čitanju iz InfluxDB")
    })
    @GetMapping("/{vehicleId}/distance")
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<DistanceDataDto> getDistanceData(
            @PathVariable Long vehicleId,
            @Parameter(description = "WEEK | MONTH | THREE_MONTHS | SIX_MONTHS | YEAR | CUSTOM") @RequestParam(defaultValue = "WEEK") String period,
            @Parameter(description = "Početni datum za CUSTOM (yyyy-MM-dd)") @RequestParam(required = false) String from,
            @Parameter(description = "Krajnji datum za CUSTOM (yyyy-MM-dd)") @RequestParam(required = false) String to
    ) {
        return ResponseEntity.ok(distanceService.getDistanceData(vehicleId, period, from, to));
    }
}