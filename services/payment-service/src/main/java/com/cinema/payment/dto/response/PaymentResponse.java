package com.cinema.payment.dto.response;

import com.cinema.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID paymentId;
    private String bookingReference;
    private BigDecimal amount;
    private PaymentStatus status;
    private String gatewayTransactionId;
    private Instant createdAt;
}

