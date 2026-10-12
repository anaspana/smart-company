package com.smartcompany.model.order;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.smartcompany.model.company.Company;
import com.smartcompany.model.user.User;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_order_customer", columnList = "customer_username"),
        @Index(name = "idx_order_company", columnList = "company_id"),
        @Index(name = "idx_order_created_at", columnList = "createdAt")
    }
)
@NoArgsConstructor @AllArgsConstructor @Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_username", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}