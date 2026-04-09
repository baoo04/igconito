package com.foodorder.deliverypayment.dto;

import jakarta.validation.constraints.NotNull;

public record StartDeliveryRequest(@NotNull Long orderId) {}
