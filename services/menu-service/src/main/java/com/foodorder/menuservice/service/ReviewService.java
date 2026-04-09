package com.foodorder.menuservice.service;

import com.foodorder.menuservice.dto.ReviewRequest;
import com.foodorder.menuservice.dto.ReviewResponse;
import com.foodorder.menuservice.entity.FoodItem;
import com.foodorder.menuservice.entity.Review;
import com.foodorder.menuservice.repository.FoodItemRepository;
import com.foodorder.menuservice.repository.ReviewRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final FoodItemRepository foodItemRepository;

    @Transactional(readOnly = true)
    public List<ReviewResponse> listForFood(Long foodId) {
        ensureFoodExists(foodId);
        return reviewRepository.findByFoodItemIdOrderByCreatedAtDesc(foodId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse addReview(Long foodId, ReviewRequest request) {
        FoodItem food = foodItemRepository.findById(foodId).orElseThrow(ReviewService::foodNotFound);
        Review r = Review.builder()
                .foodItem(food)
                .rating(request.rating())
                .comment(request.comment())
                .authorName(request.authorName() != null && !request.authorName().isBlank()
                        ? request.authorName()
                        : "Khách")
                .createdAt(Instant.now())
                .build();
        return toResponse(reviewRepository.save(r));
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getRating(), r.getComment(), r.getAuthorName(), r.getCreatedAt());
    }

    private void ensureFoodExists(Long foodId) {
        if (!foodItemRepository.existsById(foodId)) {
            throw foodNotFound();
        }
    }

    private static ResponseStatusException foodNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found");
    }
}
