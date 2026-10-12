package com.smartcompany.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(description = "Zahtev za kreiranje porudžbine")
public record CreateOrderDto(
    @Schema(description = "ID firme kojoj se proizvodi dostavljaju", example = "42")
    @NotNull(message = "Company ID je obavezan") Long companyId,

    @Schema(description = "Lista stavki porudžbine")
    @NotEmpty(message = "Porudžbina mora sadržati bar jednu stavku")
    @Valid List<OrderItemDto> items
) {
    @Schema(description = "Jedna stavka porudžbine")
    public record OrderItemDto(
        @Schema(description = "ID proizvoda", example = "7") @NotNull Long productId,
        @Schema(description = "Željena količina", example = "50")
        @NotNull @Positive(message = "Količina mora biti veća od 0") Integer quantity
    ) {}
}