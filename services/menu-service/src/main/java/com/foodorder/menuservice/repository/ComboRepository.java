package com.foodorder.menuservice.repository;

import com.foodorder.menuservice.entity.Combo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ComboRepository extends JpaRepository<Combo, Long> {

    @EntityGraph(attributePaths = {"items", "items.foodItem", "items.foodItem.category"})
    @Override
    List<Combo> findAll();

    @EntityGraph(attributePaths = {"items", "items.foodItem", "items.foodItem.category"})
    @Override
    Optional<Combo> findById(Long id);
}
