package com.foodorder.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record OrderLineRequest(
        Long menuItemId,
        Long comboId,
        @NotNull @Min(1) Integer quantity,
        @Size(max = 10) String size) {}
