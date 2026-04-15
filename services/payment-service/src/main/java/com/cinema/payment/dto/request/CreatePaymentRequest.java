package com.cinema.payment.dto.request;

import com.cinema.payment.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {
    @NotBlank
    private String bookingReference;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    private String currency;

    @NotBlank
    private String paymentMethod;

    private String cardNumber;

    @NotBlank
    private String cardHolderName;
}
