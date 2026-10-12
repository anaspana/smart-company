package com.smartcompany.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Kategorija proizvoda")
@Data @AllArgsConstructor @NoArgsConstructor
public class CategoryDto {
    @Schema(description = "ID kategorije") private Long id;
    @Schema(description = "Naziv kategorije", example = "Gume") private String name;
    @Schema(description = "Opis kategorije") private String description;
}