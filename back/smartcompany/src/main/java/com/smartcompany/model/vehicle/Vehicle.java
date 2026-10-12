package com.smartcompany.model.vehicle;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "vehicle",
    indexes = {
        @Index(name = "idx_vehicle_brand", columnList = "brand_id"),
        @Index(name = "idx_vehicle_model", columnList = "model_id"),
        @Index(name = "idx_vehicle_is_online", columnList = "isOnline"),
        @Index(name = "idx_vehicle_plate_trgm", columnList = "licensePlate")
    }
)
@NoArgsConstructor @AllArgsConstructor @Data
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String licensePlate;

    @Column(nullable = false)
    private Double weightLimit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id", nullable = false)
    private VehicleBrand brand;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_id", nullable = false)
    private VehicleModel model;

    // Poslednja poznata lokacija
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    // Simulator status
    @Column(nullable = false)
    private boolean isOnline = false;

    @Column
    private Instant lastHeartbeat;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VehicleImage> images = new ArrayList<>();
}