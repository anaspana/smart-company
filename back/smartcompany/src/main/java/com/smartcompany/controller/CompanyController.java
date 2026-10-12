package com.smartcompany.controller;

import com.smartcompany.dto.company.CompanyDecisionDto;
import com.smartcompany.dto.company.CompanyDetailDto;
import com.smartcompany.dto.company.CompanyRequestDto;
import com.smartcompany.dto.location.CityDto;
import com.smartcompany.dto.location.CountryDto;
import com.smartcompany.dto.user.UserDto;
import com.smartcompany.model.company.CompanyStatus;
import com.smartcompany.service.company.CompanyService;
import com.smartcompany.service.factory.FactoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/companies")
@Tag(name = "Companies", description = "Zahtevi za registraciju firmi i njihova obrada")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final FactoryService factoryService;

    @Operation(summary = "Kreiranje zahteva za registraciju firme", description = "Zahteva Customer rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Zahtev kreiran, status PENDING"),
        @ApiResponse(responseCode = "400", description = "Nevalidan zahtev"),
        @ApiResponse(responseCode = "404", description = "Korisnik, država ili grad nisu pronađeni"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @PostMapping
    @PreAuthorize("hasRole('Customer')")
    public ResponseEntity<CompanyDetailDto> createRequest(
            @AuthenticationPrincipal UserDto user,
            @RequestBody @Valid CompanyRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(companyService.createRequest(user.getUsername(), dto));
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

    @Operation(summary = "Pretraga zahteva za registraciju firmi", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stranica zahteva vraćena"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup"),
        @ApiResponse(responseCode = "403", description = "Nedovoljna prava pristupa")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<Page<CompanyDetailDto>> search(
            @Parameter(description = "Pretraga po nazivu firme") @RequestParam(required = false) String search,
            @Parameter(description = "Filter po statusu") @RequestParam(required = false) CompanyStatus status,
            @Parameter(description = "Broj stranice") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Veličina stranice") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Polje za sortiranje") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Smer sortiranja") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        return ResponseEntity.ok(companyService.search(search, status, PageRequest.of(page, size, sort)));
    }

    @Operation(summary = "Detalji zahteva za menadžera", description = "Zahteva Manager ili SuperAdmin rolu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Zahtev pronađen"),
        @ApiResponse(responseCode = "404", description = "Zahtev nije pronađen"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup")
    })
    @GetMapping("/{id}/manage")
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<CompanyDetailDto> getByIdForManager(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getByIdForManager(id));
    }

    @Operation(
        summary = "Obrada zahteva za registraciju firme (odobri/odbij)",
        description = "Zahteva Manager ili SuperAdmin rolu. Koristi pesimističko zaključavanje i šalje email kupcu o odluci."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Odluka uspešno upisana"),
        @ApiResponse(responseCode = "400", description = "Nedostaje razlog odbijanja kada je approve=false, ili je zahtev već obrađen"),
        @ApiResponse(responseCode = "404", description = "Zahtev nije pronađen"),
        @ApiResponse(responseCode = "401", description = "Neautorizovan pristup"),
        @ApiResponse(responseCode = "409", description = "Zahtev trenutno obrađuje drugi menadžer, pokušajte ponovo")
    })
    @PutMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('Manager', 'SuperAdmin')")
    public ResponseEntity<CompanyDetailDto> decide(
            @PathVariable Long id,
            @RequestBody @Valid CompanyDecisionDto dto) {
        return ResponseEntity.ok(companyService.decide(id, dto));
    }
}