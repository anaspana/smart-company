package com.smartcompany.model.vehicle;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "vehicle_model",
    indexes = {
        @Index(name = "idx_vehicle_model_brand", columnList = "brand_id")
    }
)
@NoArgsConstructor @AllArgsConstructor @Data
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id", nullable = false)
    private VehicleBrand brand;
}