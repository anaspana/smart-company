package com.smartcompany.model.vehicle;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicle_brand")
@NoArgsConstructor @AllArgsConstructor @Data
public class VehicleBrand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}