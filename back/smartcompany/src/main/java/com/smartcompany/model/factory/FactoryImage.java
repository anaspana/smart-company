package com.smartcompany.model.factory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "factory_image",
    indexes = {
        @Index(name = "idx_factory_image_factory", columnList = "factory_id")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class FactoryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // S3 key (filename) - isti pattern kao Product.image
    @Column(nullable = false)
    private String imageKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id", nullable = false)
    private Factory factory;
}