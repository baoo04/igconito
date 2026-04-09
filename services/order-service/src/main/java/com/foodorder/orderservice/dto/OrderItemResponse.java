package com.foodorder.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        String itemKind,
        Long menuItemId,
        String size,
        String menuItemName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {}
