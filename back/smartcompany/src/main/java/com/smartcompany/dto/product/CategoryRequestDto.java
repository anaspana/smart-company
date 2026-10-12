package com.smartcompany.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Zahtev za kreiranje/izmenu kategorije")
public record CategoryRequestDto(
    @Schema(description = "Naziv kategorije", example = "Gume") @NotBlank String name,
    @Schema(description = "Opis kategorije") String description
) {}