package com.foodorder.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MenuPriceQuotePayload(
        String kind,
        Long id,
        String name,
        boolean available,
        BigDecimal basePrice,
        BigDecimal unitPrice,
        String size,
        List<String> appliedRules) {}

