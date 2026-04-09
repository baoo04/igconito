package com.foodorder.menuservice.service;

import com.foodorder.menuservice.dto.ComboCreateRequest;
import com.foodorder.menuservice.dto.ComboItemLineRequest;
import com.foodorder.menuservice.dto.ComboItemLineResponse;
import com.foodorder.menuservice.dto.ComboResponse;
import com.foodorder.menuservice.dto.PriceQuoteResponse;
import com.foodorder.menuservice.entity.Combo;
import com.foodorder.menuservice.entity.ComboItem;
import com.foodorder.menuservice.entity.FoodItem;
import com.foodorder.menuservice.repository.ComboRepository;
import com.foodorder.menuservice.repository.FoodItemRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboRepository comboRepository;
    private final FoodItemRepository foodItemRepository;
    private final PricingService pricingService;

    @Transactional(readOnly = true)
    public List<ComboResponse> listAll() {
        return comboRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ComboResponse getById(Long id) {
        return comboRepository.findById(id).map(this::toResponse).orElseThrow(ComboService::comboNotFound);
    }

    @Transactional(readOnly = true)
    public PriceQuoteResponse quotePrice(Long id) {
        Combo combo = comboRepository.findById(id).orElseThrow(ComboService::comboNotFound);
        return pricingService.quoteCombo(combo);
    }

    @Transactional
    public ComboResponse create(ComboCreateRequest request) {
        Combo combo = Combo.builder()
                .name(request.name())
                .description(request.description())
                .bundlePrice(request.bundlePrice())
                .available(request.available())
                .items(new ArrayList<>())
                .build();
        for (ComboItemLineRequest line : request.items()) {
            FoodItem food = foodItemRepository
                    .findById(line.foodItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid foodItemId"));
            ComboItem ci = ComboItem.builder()
                    .combo(combo)
                    .foodItem(food)
                    .quantity(line.quantity())
                    .build();
            combo.getItems().add(ci);
        }
        return toResponse(comboRepository.save(combo));
    }

    private ComboResponse toResponse(Combo c) {
        List<ComboItemLineResponse> lines = c.getItems().stream()
                .map(ci -> new ComboItemLineResponse(
                        ci.getFoodItem().getId(), ci.getFoodItem().getName(), ci.getQuantity()))
                .toList();
        return new ComboResponse(
                c.getId(), c.getName(), c.getDescription(), c.getBundlePrice(), c.isAvailable(), lines);
    }

    private static ResponseStatusException comboNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Combo not found");
    }
}
