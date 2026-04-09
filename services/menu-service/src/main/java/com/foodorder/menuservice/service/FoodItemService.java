package com.foodorder.menuservice.service;

import com.foodorder.menuservice.dto.FoodItemRequest;
import com.foodorder.menuservice.dto.FoodItemResponse;
import com.foodorder.menuservice.dto.PriceQuoteResponse;
import com.foodorder.menuservice.entity.Category;
import com.foodorder.menuservice.entity.FoodItem;
import com.foodorder.menuservice.model.SizeOption;
import com.foodorder.menuservice.repository.CategoryRepository;
import com.foodorder.menuservice.repository.FoodItemRepository;
import com.foodorder.menuservice.repository.ReviewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FoodItemService {

    private final FoodItemRepository foodItemRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final PricingService pricingService;

    @Transactional(readOnly = true)
    public PriceQuoteResponse quotePrice(Long id, String sizeRaw) {
        FoodItem f = foodItemRepository.findById(id).orElseThrow(FoodItemService::foodNotFound);
        return pricingService.quoteFood(f, SizeOption.fromParam(sizeRaw));
    }

    @Transactional(readOnly = true)
    public List<FoodItemResponse> search(Long categoryId, String q, Boolean availableOnly) {
        var spec = FoodItemSpecifications.filter(categoryId, q, availableOnly);
        return foodItemRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FoodItemResponse getById(Long id) {
        FoodItem f = foodItemRepository.findById(id).orElseThrow(FoodItemService::foodNotFound);
        long rc = reviewRepository.countByFoodItem_Id(id);
        Double avg = rc == 0 ? null : reviewRepository.averageRatingByFoodId(id);
        return toResponseWithReviews(f, avg, rc);
    }

    @Transactional
    public FoodItemResponse create(FoodItemRequest request) {
        Category category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category"));
        FoodItem item = FoodItem.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .available(request.available())
                .category(category)
                .build();
        return toResponse(foodItemRepository.save(item));
    }

    @Transactional
    public FoodItemResponse update(Long id, FoodItemRequest request) {
        FoodItem item = foodItemRepository.findById(id).orElseThrow(FoodItemService::foodNotFound);
        Category category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category"));
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setAvailable(request.available());
        item.setCategory(category);
        return toResponse(foodItemRepository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        FoodItem item = foodItemRepository.findById(id).orElseThrow(FoodItemService::foodNotFound);
        foodItemRepository.delete(item);
    }

    private FoodItemResponse toResponse(FoodItem f) {
        return FoodItemResponse.withoutReviews(
                f.getId(),
                f.getName(),
                f.getDescription(),
                f.getPrice(),
                f.isAvailable(),
                f.getCategory().getId(),
                f.getCategory().getName());
    }

    private FoodItemResponse toResponseWithReviews(FoodItem f, Double averageRating, long reviewCount) {
        return new FoodItemResponse(
                f.getId(),
                f.getName(),
                f.getDescription(),
                f.getPrice(),
                f.isAvailable(),
                f.getCategory().getId(),
                f.getCategory().getName(),
                averageRating,
                reviewCount);
    }

    private static ResponseStatusException foodNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found");
    }
}
