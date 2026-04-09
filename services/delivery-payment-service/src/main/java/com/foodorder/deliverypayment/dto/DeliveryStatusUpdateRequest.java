package com.foodorder.deliverypayment.dto;

import com.foodorder.deliverypayment.entity.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record DeliveryStatusUpdateRequest(@NotNull DeliveryStatus status) {}
