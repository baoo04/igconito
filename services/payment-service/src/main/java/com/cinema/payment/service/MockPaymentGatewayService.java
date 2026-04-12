package com.cinema.payment.service;

import com.cinema.payment.dto.CreatePaymentRequest;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Service;

@Service
public class MockPaymentGatewayService {

  private static final BigDecimal MAX_AUTO_APPROVE = new BigDecimal("10000000");

  public PaymentGatewayResult charge(CreatePaymentRequest request) {
    String digits = request.getCardNumber() != null ? request.getCardNumber().replaceAll("\\s+", "") : "";
    if (digits.endsWith("0000")) {
      return fail("Card ending 0000 is declined (mock rule)");
    }
    if (request.getAmount() != null && request.getAmount().compareTo(MAX_AUTO_APPROVE) > 0) {
      return fail("Amount exceeds mock limit (10,000,000 VND)");
    }
    boolean success = ThreadLocalRandom.current().nextInt(100) < 90;
    if (!success) {
      return fail("Random decline (mock 10% failure rate)");
    }
    return PaymentGatewayResult.builder()
        .success(true)
        .gatewayTransactionId("MOCK-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase())
        .rawResponseJson("{\"status\":\"SUCCESS\",\"mock\":true}")
        .build();
  }

  private static PaymentGatewayResult fail(String reason) {
    return PaymentGatewayResult.builder()
        .success(false)
        .rawResponseJson("{\"status\":\"FAILED\",\"reason\":\"" + reason.replace("\"", "'") + "\"}")
        .build();
  }

  @Value
  @Builder
  public static class PaymentGatewayResult {
    boolean success;
    String gatewayTransactionId;
    String rawResponseJson;
  }
}
