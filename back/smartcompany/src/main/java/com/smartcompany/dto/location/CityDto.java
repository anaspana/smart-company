package com.smartcompany.dto.location;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Grad")
public record CityDto(
    @Schema(description = "ID grada") Long id,
    @Schema(description = "Naziv grada", example = "Novi Sad") String name,
    @Schema(description = "ID države kojoj grad pripada") Long countryId
) {}