package com.smartcompany.dto.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.List;

/**
 * Plain, Jackson-friendly obertač oko Page<T> za keširanje u Redisu.
 * PageImpl se namerno NE kešira direktno — nema odgovarajući konstruktor za
 * Jackson (Pageable je i sam interfejs), pa bi svaki pokušaj polimorfne
 * (de)serijalizacije bio krt. Ovaj record čuva samo ono što je stvarno
 * potrebno da se Page rekonstruiše nakon čitanja iz keša.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public record CachedPage<T>(List<T> content, int number, int size, long totalElements) implements Serializable {

    public static <T> CachedPage<T> from(Page<T> page) {
        return new CachedPage<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public Page<T> toPage(Pageable pageable) {
        return new PageImpl<>(content, pageable, totalElements);
    }
}