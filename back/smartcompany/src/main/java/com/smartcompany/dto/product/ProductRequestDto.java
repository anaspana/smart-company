package com.smartcompany.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Zahtev za kreiranje/izmenu proizvoda")
public record ProductRequestDto(
    @Schema(description = "Naziv proizvoda", example = "Guma 195/55/R15-H") @NotBlank String name,
    @Schema(description = "Opis proizvoda") String description,
    @Schema(description = "Cena", example = "89.99") @NotNull @Positive BigDecimal price,
    @Schema(description = "Težina u kg", example = "8.5") @NotNull @Positive Double weight,
    @Schema(description = "S3 ključ slike") String image,
    @Schema(description = "Da li je proizvod na prodaji") boolean isForSale,
    @Schema(description = "ID kategorije") @NotNull Long categoryId,
    @Schema(description = "ID-jevi fabrika koje proizvode ovaj proizvod") List<Long> factoryIds
) {}