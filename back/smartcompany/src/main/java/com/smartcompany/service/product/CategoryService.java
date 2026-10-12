package com.smartcompany.service.product;

import com.smartcompany.dto.product.CategoryDto;
import com.smartcompany.dto.product.CategoryRequestDto;
import com.smartcompany.exception.AppException;
import com.smartcompany.model.product.Category;
import com.smartcompany.repository.product.CategoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Cacheable("categories")
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public CategoryDto getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Category not found", HttpStatus.NOT_FOUND));
        return toDto(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDto create(CategoryRequestDto dto) {
        if (categoryRepository.existsByName(dto.name())) {
            throw new AppException("Category with that name already exists", HttpStatus.BAD_REQUEST);
        }
        Category category = new Category(null, dto.name(), dto.description());
        return toDto(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDto update(Long id, CategoryRequestDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Category not found", HttpStatus.NOT_FOUND));
        category.setName(dto.name());
        category.setDescription(dto.description());
        return toDto(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new AppException("Category not found", HttpStatus.NOT_FOUND);
        }
        categoryRepository.deleteById(id);
    }

    public CategoryDto toDto(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getDescription());
    }

    public Category toEntity(CategoryDto dto) {
        return new Category(dto.getId(), dto.getName(), dto.getDescription());
    }
}