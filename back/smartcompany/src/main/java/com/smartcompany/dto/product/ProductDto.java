package com.smartcompany.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

import com.smartcompany.dto.factory.FactorySummaryDto;

@Schema(description = "Proizvod")
@Data @AllArgsConstructor @NoArgsConstructor
public class ProductDto {
    @Schema(description = "ID proizvoda") private Long id;
    @Schema(description = "Naziv proizvoda") private String name;
    @Schema(description = "Opis proizvoda") private String description;
    @Schema(description = "Cena proizvoda") private BigDecimal price;
    @Schema(description = "Težina proizvoda u kg") private Double weight;
    @Schema(description = "S3 ključ slike proizvoda") private String image;
    @Schema(description = "Da li je proizvod trenutno na prodaji") private boolean isForSale;
    @Schema(description = "Kategorija proizvoda") private CategoryDto category;
    @Schema(description = "Ukupna zaliha proizvoda u svim magacinima") private Integer totalStock;
    @Schema(description = "Fabrike koje proizvode ovaj proizvod (samo kod getById)") private List<FactorySummaryDto> factories;
}