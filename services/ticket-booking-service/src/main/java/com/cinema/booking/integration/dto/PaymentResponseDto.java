package com.cinema.booking.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResponseDto {

  private UUID paymentId;
  private String bookingReference;
  private BigDecimal amount;
  private String status;
  private String gatewayTransactionId;
  private Instant createdAt;
}
