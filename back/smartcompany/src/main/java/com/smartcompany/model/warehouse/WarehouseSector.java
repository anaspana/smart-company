package com.smartcompany.model.warehouse;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "warehouse_sector",
    indexes = {
        @Index(name = "idx_sector_warehouse", columnList = "warehouse_id")
    }
)
@NoArgsConstructor @AllArgsConstructor @Data
public class WarehouseSector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private String name; // npr. "Sektor A", "Hladnjača"

    // Poslednja izmerena temperatura (čuva se u relacionoj bazi)
    @Column
    private Double lastTemperature;

    @Column
    private java.time.Instant lastMeasuredAt;
}