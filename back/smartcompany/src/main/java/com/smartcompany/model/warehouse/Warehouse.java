package com.smartcompany.model.warehouse;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.smartcompany.model.location.City;
import com.smartcompany.model.location.Country;

@Entity
@Table(
    name = "warehouse",
    indexes = {
        @Index(name = "idx_warehouse_city", columnList = "city_id"),
        @Index(name = "idx_warehouse_country", columnList = "country_id"),
        @Index(name = "idx_warehouse_is_online", columnList = "isOnline")
    }
)
@NoArgsConstructor @AllArgsConstructor @Data
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private boolean isOnline = false;

    @Column
    private Instant lastHeartbeat;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WarehouseImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WarehouseItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WarehouseSector> sectors = new ArrayList<>();
}