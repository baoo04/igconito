package com.foodorder.orderservice.dto;

import com.foodorder.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(@NotNull OrderStatus status) {}
