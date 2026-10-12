package com.smartcompany.dto.factory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Zahtev za kreiranje/izmenu fabrike")
public record FactoryRequestDto(
    @Schema(description = "Naziv fabrike", example = "Fabrika guma Novi Sad") @NotBlank String name,
    @Schema(description = "Adresa fabrike") String address,
    @Schema(description = "ID države") @NotNull Long countryId,
    @Schema(description = "ID grada") @NotNull Long cityId,
    @Schema(description = "Geografska širina") @NotNull Double latitude,
    @Schema(description = "Geografska dužina") @NotNull Double longitude,
    @Schema(description = "S3 ključevi uploadovanih slika") List<String> imageKeys,
    @Schema(description = "ID-jevi proizvoda koje fabrika proizvodi") List<Long> productIds
) {}