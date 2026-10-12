package com.smartcompany.model.vehicle;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicle_image")
@NoArgsConstructor @AllArgsConstructor @Data
public class VehicleImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private String imageKey;
}