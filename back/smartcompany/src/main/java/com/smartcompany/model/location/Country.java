package com.smartcompany.model.location;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "country")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 3)
    private String code; // ISO 3166-1 alpha-2, npr. "RS", "DE"
}