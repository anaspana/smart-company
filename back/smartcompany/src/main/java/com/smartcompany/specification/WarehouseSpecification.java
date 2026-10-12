package com.smartcompany.specification;

import com.smartcompany.model.warehouse.Warehouse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class WarehouseSpecification {

    public static Specification<Warehouse> search(String search, Long countryId,
                                                   Long cityId, Boolean isOnline) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
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