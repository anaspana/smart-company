package com.smartcompany.model.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
    name = "product",
    indexes = {
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_is_for_sale", columnList = "isForSale"),
        @Index(name = "idx_product_price", columnList = "price"),
        @Index(name = "idx_product_category_sale", columnList = "category_id, isForSale")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Double weight;

    @Column
    private String image;

    @Column(nullable = false)
    private boolean isForSale = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}