package com.smartcompany.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Odgovor sa detaljima kreirane porudžbine")
public record OrderResponseDto(
    @Schema(description = "ID porudžbine") Long id,
    @Schema(description = "Vreme kreiranja (ISO-8601)") String createdAt,
    @Schema(description = "Ukupna cena") BigDecimal totalPrice,
    @Schema(description = "ID firme kojoj se dostavlja") Long companyId,
    @Schema(description = "Naziv firme kojoj se dostavlja") String companyName,
    @Schema(description = "Stavke porudžbine") List<OrderItemResponseDto> items
) {
    public record OrderItemResponseDto(
        @Schema(description = "ID proizvoda") Long productId,
        @Schema(description = "Naziv proizvoda") String productName,
        @Schema(description = "Poručena količina") Integer quantity,
        @Schema(description = "Cena u trenutku porudžbine") BigDecimal priceAtOrder
    ) {}
}