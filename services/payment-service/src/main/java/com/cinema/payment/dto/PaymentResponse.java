package com.cinema.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {

  private UUID paymentId;
  private String bookingReference;
  private BigDecimal amount;
  private String status;
  private String gatewayTransactionId;
  private Instant createdAt;
}
