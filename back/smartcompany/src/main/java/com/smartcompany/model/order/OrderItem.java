package com.smartcompany.model.order;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

import com.smartcompany.model.product.Product;

@Entity
@Table(
    name = "order_item",
    indexes = {
        @Index(name = "idx_oitem_order", columnList = "order_id"),
        @Index(name = "idx_oitem_product", columnList = "product_id")
    }
)
@NoArgsConstructor @AllArgsConstructor @Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtOrder; // cena u momentu porudžbine
}