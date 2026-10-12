package com.smartcompany.dto.company;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import com.smartcompany.dto.location.CityDto;
import com.smartcompany.dto.location.CountryDto;

@Schema(description = "Detalji zahteva za registraciju firme")
public record CompanyDetailDto(
    @Schema(description = "ID firme") Long id,
    @Schema(description = "Naziv firme") String name,
    @Schema(description = "Adresa firme") String address,
    @Schema(description = "Država") CountryDto country,
    @Schema(description = "Grad") CityDto city,
    @Schema(description = "Geografska širina") Double latitude,
    @Schema(description = "Geografska dužina") Double longitude,
    @Schema(description = "Status zahteva", allowableValues = {"PENDING", "APPROVED", "REJECTED"}) String status,
    @Schema(description = "Razlog odbijanja") String rejectionReason,
    @Schema(description = "Korisničko ime vlasnika") String ownerUsername,
    @Schema(description = "S3 ključevi slika firme") List<String> imageKeys,
    @Schema(description = "Priloženi dokumenti") List<DocumentDto> documents
) {
    @Schema(description = "Priloženi dokument")
    public record DocumentDto(
        @Schema(description = "S3 ključ fajla") String fileKey,
        @Schema(description = "Tip fajla", allowableValues = {"image", "pdf"}) String fileType
    ) {}
}