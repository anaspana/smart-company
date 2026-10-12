package com.smartcompany.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcompany.model.product.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
}