package com.foodorder.deliverypayment.repository;

import com.foodorder.deliverypayment.entity.Delivery;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrderId(Long orderId);
}
