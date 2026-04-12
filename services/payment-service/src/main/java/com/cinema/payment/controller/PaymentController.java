package com.cinema.payment.controller;

import com.cinema.payment.dto.ApiResponse;
import com.cinema.payment.dto.CreatePaymentRequest;
import com.cinema.payment.dto.PaymentResponse;
import com.cinema.payment.entity.PaymentStatus;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody CreatePaymentRequest request) {
    PaymentTransaction tx = paymentService.processPayment(request);
    PaymentResponse body = paymentService.toResponse(tx);
    if (tx.getStatus() == PaymentStatus.SUCCESS) {
      return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(body));
    }
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
        .body(
            ApiResponse.<PaymentResponse>builder()
                .data(body)
                .message("Payment declined")
                .status(402)
                .build());
  }

  @GetMapping("/{paymentId}")
  public ResponseEntity<ApiResponse<PaymentResponse>> get(@PathVariable UUID paymentId) {
    return ResponseEntity.ok(ApiResponse.ok(paymentService.get(paymentId)));
  }

  @PostMapping("/{paymentId}/confirm")
  public ResponseEntity<ApiResponse<PaymentResponse>> confirm(@PathVariable UUID paymentId) {
    return ResponseEntity.ok(ApiResponse.ok(paymentService.confirm(paymentId)));
  }
}
