package com.foodorder.menuservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record PriceQuoteResponse(
        String kind,
        Long id,
        String name,
        boolean available,
        BigDecimal basePrice,
        BigDecimal unitPrice,
        String size,
        List<String> appliedRules) {}
