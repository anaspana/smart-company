package com.smartcompany.model.warehouse;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "warehouse_image")
@NoArgsConstructor @AllArgsConstructor @Data
public class WarehouseImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private String imageKey;
}