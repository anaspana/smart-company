package com.smartcompany.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(description = "Zahtev za kreiranje/izmenu dostavnog vozila")
public record VehicleRequestDto(
    @Schema(description = "Registarska oznaka", example = "NS-123-AB") @NotBlank String licensePlate,
    @Schema(description = "Ograničenje težine u kg", example = "3500") @NotNull @Positive Double weightLimit,
    @Schema(description = "ID marke") @NotNull Long brandId,
    @Schema(description = "ID modela") @NotNull Long modelId,
    @Schema(description = "S3 ključevi uploadovanih slika") List<String> imageKeys
) {}