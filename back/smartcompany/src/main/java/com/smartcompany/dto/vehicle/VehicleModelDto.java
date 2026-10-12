package com.smartcompany.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Model vozila")
public record VehicleModelDto(
    @Schema(description = "ID modela") Long id,
    @Schema(description = "Naziv modela", example = "Sprinter") String name,
    @Schema(description = "ID marke") Long brandId
) {}