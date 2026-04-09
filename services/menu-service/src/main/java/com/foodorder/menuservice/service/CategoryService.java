package com.foodorder.menuservice.service;

import com.foodorder.menuservice.dto.CategoryRequest;
import com.foodorder.menuservice.dto.CategoryResponse;
import com.foodorder.menuservice.entity.Category;
import com.foodorder.menuservice.repository.CategoryRepository;
import com.foodorder.menuservice.repository.FoodItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final FoodItemRepository foodItemRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return categoryRepository.findById(id).map(this::toResponse).orElseThrow(CategoryService::notFound);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category c = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();
        return toResponse(categoryRepository.save(c));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category c = categoryRepository.findById(id).orElseThrow(CategoryService::notFound);
        c.setName(request.name());
        c.setDescription(request.description());
        return toResponse(categoryRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        Category c = categoryRepository.findById(id).orElseThrow(CategoryService::notFound);
        if (foodItemRepository.countByCategoryId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category has food items");
        }
        categoryRepository.delete(c);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
    }
}
