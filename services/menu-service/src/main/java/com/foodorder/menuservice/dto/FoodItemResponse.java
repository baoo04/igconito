package com.foodorder.menuservice.dto;

import java.math.BigDecimal;

public record FoodItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        boolean available,
        Long categoryId,
        String categoryName,
        Double averageRating,
        Long reviewCount) {

    public static FoodItemResponse withoutReviews(
            Long id,
            String name,
            String description,
            BigDecimal price,
            boolean available,
            Long categoryId,
            String categoryName) {
        return new FoodItemResponse(id, name, description, price, available, categoryId, categoryName, null, null);
    }
}
