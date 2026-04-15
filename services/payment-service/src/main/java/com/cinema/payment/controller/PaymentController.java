package com.cinema.payment.controller;

import com.cinema.payment.dto.request.CreatePaymentRequest;
import com.cinema.payment.dto.response.ApiResponse;
import com.cinema.payment.dto.response.PaymentResponse;
import com.cinema.payment.entity.PaymentStatus;
import com.cinema.payment.exception.ErrorCode;
import com.cinema.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> create(
        @Valid @RequestBody CreatePaymentRequest request,
        @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        PaymentResponse response = paymentService.processPayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> get(@PathVariable UUID paymentId) {
       return ResponseEntity.ok(ApiResponse.ok(paymentService.get(paymentId)));
    }
}
