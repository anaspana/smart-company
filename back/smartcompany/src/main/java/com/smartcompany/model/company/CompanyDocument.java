package com.smartcompany.model.company;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company_document")
@NoArgsConstructor @AllArgsConstructor @Data
public class CompanyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String fileKey;

    @Column(nullable = false)
    private String fileType; // "image" ili "pdf"
}