package com.cinema.payment.service;

import com.cinema.payment.dto.CreatePaymentRequest;
import com.cinema.payment.dto.PaymentResponse;
import com.cinema.payment.entity.PaymentMethod;
import com.cinema.payment.entity.PaymentStatus;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.exception.ResourceNotFoundException;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.MockPaymentGatewayService.PaymentGatewayResult;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentTransactionRepository repository;
  private final MockPaymentGatewayService mockGateway;

  @Transactional
  public PaymentTransaction processPayment(CreatePaymentRequest req) {
    PaymentMethod method =
        PaymentMethod.valueOf(req.getPaymentMethod().trim().toUpperCase(Locale.ROOT));
    String currency = req.getCurrency() != null ? req.getCurrency().trim() : "VND";
    String lastFour = extractLastFour(req.getCardNumber());

    PaymentTransaction tx =
        PaymentTransaction.builder()
            .bookingReference(req.getBookingReference().trim())
            .amount(req.getAmount())
            .currency(currency)
            .status(PaymentStatus.PENDING)
            .paymentMethod(method)
            .cardLastFour(lastFour)
            .build();
    tx = repository.save(tx);

    PaymentGatewayResult result = mockGateway.charge(req);
    tx.setGatewayResponse(result.getRawResponseJson());
    if (result.isSuccess()) {
      tx.setStatus(PaymentStatus.SUCCESS);
      tx.setGatewayTransactionId(result.getGatewayTransactionId());
    } else {
      tx.setStatus(PaymentStatus.FAILED);
    }
    return repository.save(tx);
  }

  @Transactional(readOnly = true)
  public PaymentResponse get(UUID id) {
    return toResponse(
        repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id)));
  }

  @Transactional
  public PaymentResponse confirm(UUID id) {
    PaymentTransaction tx =
        repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    if (tx.getStatus() != PaymentStatus.PENDING) {
      throw new IllegalStateException("Payment not in PENDING state: " + tx.getStatus());
    }
    tx.setStatus(PaymentStatus.SUCCESS);
    tx.setGatewayTransactionId(
        tx.getGatewayTransactionId() != null
            ? tx.getGatewayTransactionId()
            : "CONFIRM-" + UUID.randomUUID());
    return toResponse(tx);
  }

  private static String extractLastFour(String cardNumber) {
    if (cardNumber == null || cardNumber.isBlank()) {
      return null;
    }
    String d = cardNumber.replaceAll("\\D", "");
    if (d.length() < 4) {
      return d;
    }
    return d.substring(d.length() - 4);
  }

  public PaymentResponse toResponse(PaymentTransaction tx) {
    return PaymentResponse.builder()
        .paymentId(tx.getId())
        .bookingReference(tx.getBookingReference())
        .amount(tx.getAmount())
        .status(tx.getStatus().name())
        .gatewayTransactionId(tx.getGatewayTransactionId())
        .createdAt(tx.getCreatedAt())
        .build();
  }
}
