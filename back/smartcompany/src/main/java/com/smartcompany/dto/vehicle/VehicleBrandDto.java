package com.smartcompany.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Marka vozila")
public record VehicleBrandDto(
    @Schema(description = "ID marke") Long id,
    @Schema(description = "Naziv marke", example = "Mercedes-Benz") String name
) {}