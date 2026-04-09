package com.foodorder.deliverypayment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MockPaymentRequest(@NotNull Long orderId, @NotNull @DecimalMin("0.01") BigDecimal amount) {}
