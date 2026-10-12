package com.smartcompany.dto.company;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Skraćeni prikaz odobrene firme (dropdown u formi porudžbine)")
public record CompanyDto(
    @Schema(description = "ID firme") Long id,
    @Schema(description = "Naziv firme") String name,
    @Schema(description = "Grad") String city,
    @Schema(description = "Država") String country
) {}