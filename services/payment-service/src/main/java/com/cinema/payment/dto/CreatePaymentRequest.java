package com.cinema.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreatePaymentRequest {

  @NotBlank private String bookingReference;

  @NotNull @DecimalMin("0.01")
  private BigDecimal amount;

  private String currency;

  @NotBlank private String paymentMethod;

  private String cardNumber;
  @NotBlank private String cardHolderName;
}
