package com.foodorder.menuservice.repository;

import com.foodorder.menuservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
