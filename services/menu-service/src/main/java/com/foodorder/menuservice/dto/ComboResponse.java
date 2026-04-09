package com.foodorder.menuservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record ComboResponse(
        Long id,
        String name,
        String description,
        BigDecimal bundlePrice,
        boolean available,
        List<ComboItemLineResponse> items) {}
