package com.smartcompany.specification;

import com.smartcompany.model.vehicle.Vehicle;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class VehicleSpecification {

    public static Specification<Vehicle> search(String search, Long brandId,
                                                  Long modelId, Boolean isOnline) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("licensePlate")), pattern));
            }
            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), brandId));
            }
            if (modelId != null) {
                predicates.add(cb.equal(root.get("model").get("id"), modelId));
            }
            if (isOnline != null) {
                predicates.add(cb.equal(root.get("isOnline"), isOnline));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}