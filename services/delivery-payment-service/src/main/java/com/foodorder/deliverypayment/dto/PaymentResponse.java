package com.foodorder.deliverypayment.dto;

import com.foodorder.deliverypayment.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(Long id, Long orderId, BigDecimal amount, PaymentStatus status, Instant createdAt) {}
