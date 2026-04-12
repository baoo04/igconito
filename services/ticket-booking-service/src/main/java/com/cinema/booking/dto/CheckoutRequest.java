package com.cinema.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class CheckoutRequest {

  @NotNull private UUID holdId;

  @NotNull @Positive private Long showtimeId;

  @Valid @NotNull private Customer customer;

  @Valid @NotNull private Payment payment;

  @Data
  public static class Customer {
    @NotBlank private String fullName;
    @NotBlank private String email;
    @NotBlank private String phone;
  }

  @Data
  public static class Payment {
    @NotBlank private String method;
    private String cardNumber;
    @NotBlank private String cardHolderName;
    @NotNull @Positive private BigDecimal totalAmount;
  }
}
