package com.cinema.payment.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentGatewayResult {
    boolean success;
    String gatewayTransactionId;
    String rawResponseJson;
}
