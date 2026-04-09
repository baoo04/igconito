package com.foodorder.menuservice.repository;

import com.foodorder.menuservice.entity.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByFoodItemIdOrderByCreatedAtDesc(Long foodItemId);

    long countByFoodItem_Id(Long foodItemId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.foodItem.id = :foodId")
    Double averageRatingByFoodId(@Param("foodId") Long foodId);
}
