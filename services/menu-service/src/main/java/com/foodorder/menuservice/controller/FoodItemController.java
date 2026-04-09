package com.foodorder.menuservice.controller;

import com.foodorder.menuservice.dto.FoodItemRequest;
import com.foodorder.menuservice.dto.FoodItemResponse;
import com.foodorder.menuservice.dto.PriceQuoteResponse;
import com.foodorder.menuservice.dto.ReviewRequest;
import com.foodorder.menuservice.dto.ReviewResponse;
import com.foodorder.menuservice.service.FoodItemService;
import com.foodorder.menuservice.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/foods")
@RequiredArgsConstructor
public class FoodItemController {

    private final FoodItemService foodItemService;
    private final ReviewService reviewService;

    @GetMapping("/{id}/price")
    public PriceQuoteResponse getPrice(@PathVariable Long id, @RequestParam(defaultValue = "M") String size) {
        return foodItemService.quotePrice(id, size);
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewResponse> listReviews(@PathVariable Long id) {
        return reviewService.listForFood(id);
    }

    @PostMapping("/{id}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse addReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return reviewService.addReview(id, request);
    }

    @GetMapping
    public List<FoodItemResponse> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean availableOnly) {
        return foodItemService.search(categoryId, q, availableOnly);
    }

    @GetMapping("/{id:\\d+}")
    public FoodItemResponse get(@PathVariable Long id) {
        return foodItemService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodItemResponse create(@Valid @RequestBody FoodItemRequest request) {
        return foodItemService.create(request);
    }

    @PutMapping("/{id}")
    public FoodItemResponse update(@PathVariable Long id, @Valid @RequestBody FoodItemRequest request) {
        return foodItemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        foodItemService.delete(id);
    }
}
