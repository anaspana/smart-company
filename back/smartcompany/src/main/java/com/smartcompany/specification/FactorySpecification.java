package com.smartcompany.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import com.smartcompany.model.factory.Factory;

import java.util.ArrayList;
import java.util.List;

public class FactorySpecification {

    public static Specification<Factory> search(String search, Long countryId, Long cityId, Boolean isOnline) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                // trigram indeks na name LIKE '%x%' je OK uz pg_trgm GIN indeks
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }
            if (countryId != null) {
                predicates.add(cb.equal(root.get("country").get("id"), countryId));
            }
            if (cityId != null) {
                predicates.add(cb.equal(root.get("city").get("id"), cityId));
            }
            if (isOnline != null) {
                predicates.add(cb.equal(root.get("isOnline"), isOnline));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}