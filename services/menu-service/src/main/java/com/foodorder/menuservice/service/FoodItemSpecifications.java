package com.foodorder.menuservice.service;

import com.foodorder.menuservice.entity.FoodItem;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class FoodItemSpecifications {

    private FoodItemSpecifications() {}

    public static Specification<FoodItem> filter(Long categoryId, String search, Boolean availableOnly) {
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (categoryId != null) {
                parts.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                parts.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), pattern),
                                cb.like(cb.lower(root.get("description")), pattern)));
            }
            if (Boolean.TRUE.equals(availableOnly)) {
                parts.add(cb.isTrue(root.get("available")));
            }
            return parts.isEmpty() ? cb.conjunction() : cb.and(parts.toArray(Predicate[]::new));
        };
    }
}
