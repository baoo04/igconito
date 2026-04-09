package com.foodorder.deliverypayment.service;

import com.foodorder.deliverypayment.dto.MockPaymentRequest;
import com.foodorder.deliverypayment.dto.PaymentResponse;
import com.foodorder.deliverypayment.entity.Payment;
import com.foodorder.deliverypayment.entity.PaymentStatus;
import com.foodorder.deliverypayment.repository.PaymentRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse mockPay(MockPaymentRequest request) {
        Payment p = Payment.builder()
                .orderId(request.orderId())
                .amount(request.amount())
                .status(PaymentStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
        return toResponse(paymentRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listByOrder(Long orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse latestByOrder(Long orderId) {
        return paymentRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .map(this::toResponse)
                .orElse(null);
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getStatus(), p.getCreatedAt());
    }
}
