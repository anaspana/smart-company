package com.smartcompany.model.factory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import com.smartcompany.model.product.Product;

@Entity
@Table(
    name = "production_record",
    indexes = {
        @Index(name = "idx_prod_record_factory", columnList = "factory_id"),
        @Index(name = "idx_prod_record_product", columnList = "product_id"),
        @Index(name = "idx_prod_record_timestamp", columnList = "timestamp"),
        @Index(name = "idx_prod_record_factory_product_ts", columnList = "factory_id, product_id, timestamp")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProductionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id", nullable = false)
    private Factory factory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Instant timestamp;
}