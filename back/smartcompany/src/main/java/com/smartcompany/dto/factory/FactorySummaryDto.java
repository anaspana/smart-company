package com.smartcompany.dto.factory;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Skraćeni prikaz fabrike")
public record FactorySummaryDto(
    @Schema(description = "ID fabrike") Long id,
    @Schema(description = "Naziv fabrike") String name
) {}