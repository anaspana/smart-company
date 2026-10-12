package com.smartcompany.dto.location;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Država")
public record CountryDto(
    @Schema(description = "ID države") Long id,
    @Schema(description = "Naziv države", example = "Srbija") String name,
    @Schema(description = "ISO kod države", example = "RS") String code
) {}