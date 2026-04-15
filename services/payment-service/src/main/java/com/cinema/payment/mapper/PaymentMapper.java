package com.cinema.payment.mapper;

import com.cinema.payment.dto.response.PaymentResponse;
import com.cinema.payment.entity.PaymentTransaction;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResponse toResponse(PaymentTransaction tx) {
        return PaymentResponse.builder()
            .paymentId(tx.getId())
            .bookingReference(tx.getBookingReference())
            .amount(tx.getAmount())
            .status(tx.getStatus())
            .gatewayTransactionId(tx.getGatewayTransactionId())
            .createdAt(tx.getCreatedAt())
            .build();
    }
}
