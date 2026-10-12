package com.smartcompany.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import com.smartcompany.model.user.User;
import com.smartcompany.model.user.UserType;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> searchManagers(String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Uvek filtriramo samo menadžere
            predicates.add(cb.equal(root.get("type"), UserType.Manager));

            // Pretraga po pocetku username-a koristi B-tree indeks
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("username")),
                    search.toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}