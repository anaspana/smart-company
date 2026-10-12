package com.smartcompany.model.company;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

import com.smartcompany.model.location.City;
import com.smartcompany.model.location.Country;
import com.smartcompany.model.user.User;

@Entity
@Table(
    name = "company",
    indexes = {
        @Index(name = "idx_company_owner", columnList = "owner_username"),
        @Index(name = "idx_company_status", columnList = "status")
    }
)
@NoArgsConstructor @AllArgsConstructor @Data
public class Company {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_username", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status = CompanyStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompanyDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompanyImage> images = new ArrayList<>();
}