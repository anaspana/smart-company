package com.smartcompany.dto.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Zahtev za kreiranje/izmenu magacina")
public record WarehouseRequestDto(
    @Schema(description = "Naziv magacina") @NotBlank String name,
    @Schema(description = "Adresa magacina") String address,
    @Schema(description = "ID države") @NotNull Long countryId,
    @Schema(description = "ID grada") @NotNull Long cityId,
    @Schema(description = "Geografska širina") @NotNull Double latitude,
    @Schema(description = "Geografska dužina") @NotNull Double longitude,
    @Schema(description = "S3 ključevi uploadovanih slika") List<String> imageKeys,
    @Schema(description = "Sektori magacina") List<SectorRequestDto> sectors
) {
    @Schema(description = "Sektor magacina")
    public record SectorRequestDto(
        @Schema(description = "Naziv sektora", example = "Hladna komora A") @NotBlank String name
    ) {}
}