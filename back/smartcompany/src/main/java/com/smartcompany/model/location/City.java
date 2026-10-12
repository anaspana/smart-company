package com.smartcompany.model.location;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "city",
    indexes = {
        @Index(name = "idx_city_country", columnList = "country_id")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;
}