package com.smartcompany.dto.company;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Zahtev za registraciju firme")
public record CompanyRequestDto(
    @Schema(description = "Naziv firme", example = "Auto Delovi DOO") @NotBlank String name,
    @Schema(description = "Adresa firme") String address,
    @Schema(description = "ID države") @NotNull Long countryId,
    @Schema(description = "ID grada") @NotNull Long cityId,
    @Schema(description = "Geografska širina") @NotNull Double latitude,
    @Schema(description = "Geografska dužina") @NotNull Double longitude,
    @Schema(description = "S3 ključevi slika firme") List<String> imageKeys,
    @Schema(description = "Dokumenti kao dokaz o vlasništvu") List<DocumentDto> documents
) {
    @Schema(description = "Priloženi dokument")
    public record DocumentDto(
        @Schema(description = "S3 ključ fajla") @NotBlank String fileKey,
        @Schema(description = "Tip fajla", allowableValues = {"image", "pdf"}) @NotBlank String fileType
    ) {}
}