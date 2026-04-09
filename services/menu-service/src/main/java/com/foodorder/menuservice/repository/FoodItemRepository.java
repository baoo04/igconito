package com.foodorder.menuservice.repository;

import com.foodorder.menuservice.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long>, JpaSpecificationExecutor<FoodItem> {

    long countByCategoryId(Long categoryId);
}
