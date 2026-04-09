package com.foodorder.orderservice.repository;

import com.foodorder.orderservice.entity.CustomerOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = "items")
    @Override
    Optional<CustomerOrder> findById(Long id);

    @EntityGraph(attributePaths = "items")
    @Override
    List<CustomerOrder> findAll();
}
