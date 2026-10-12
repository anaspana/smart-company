package com.smartcompany.model.factory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.smartcompany.model.location.City;
import com.smartcompany.model.location.Country;
import com.smartcompany.model.product.Product;

@Entity
@Table(
    name = "factory",
    indexes = {
        @Index(name = "idx_factory_country", columnList = "country_id"),
        @Index(name = "idx_factory_city", columnList = "city_id"),
        @Index(name = "idx_factory_is_online", columnList = "isOnline")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Factory {

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

    // Simulator status
    @Column(nullable = false)
    private boolean isOnline = false;

    @Column
    private Instant lastHeartbeat;

    @OneToMany(mappedBy = "factory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FactoryImage> images = new ArrayList<>();

    // Koje proizvode fabrika proizvodi
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "factory_product",
        joinColumns = @JoinColumn(name = "factory_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> products = new ArrayList<>();
}