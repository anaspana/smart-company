package com.smartcompany.model.warehouse;

import com.smartcompany.model.product.Product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "warehouse_item",
    indexes = {
        @Index(name = "idx_witem_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_witem_product", columnList = "product_id"),
        @Index(name = "idx_witem_warehouse_product", columnList = "warehouse_id, product_id")
    }
)
@NoArgsConstructor @AllArgsConstructor @Data
public class WarehouseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity = 0;
}