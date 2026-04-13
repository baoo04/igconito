package com.cinema.payment.service;

import com.cinema.payment.dto.request.CreatePaymentRequest;
import com.cinema.payment.dto.response.PaymentResponse;
import com.cinema.payment.entity.PaymentGatewayResult;
import com.cinema.payment.entity.PaymentMethod;
import com.cinema.payment.entity.PaymentStatus;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.exception.AppException;
import com.cinema.payment.exception.ErrorCode;
import com.cinema.payment.mapper.PaymentMapper;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.utils.CardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentTransactionRepository repository;
    private final MockPaymentGatewayService mockGateway;

    // mapper
    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentResponse processPayment(CreatePaymentRequest req, String key) {
        // solve idempotency
        if (key == null || key.isBlank()) {
            throw new AppException(ErrorCode.IDEMPOTENCY_REQUIRED);
        }

        var existingOpt = repository.findByIdempotencyKey(key);

        if (existingOpt.isPresent()) {
            PaymentTransaction existing = existingOpt.get();
            // always return same record
            return paymentMapper.toResponse(existing);
        }

        PaymentMethod method = PaymentMethod.valueOf(req.getPaymentMethod().toUpperCase());
        String currency = req.getCurrency() != null ? req.getCurrency().trim() : "VND";
        String lastFour = CardUtils.extractLastFour(req.getCardNumber());

        PaymentTransaction tx =
            PaymentTransaction.builder()
                .bookingReference(req.getBookingReference().trim())
                .amount(req.getAmount())
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .paymentMethod(method)
                .cardLastFour(lastFour)
                .idempotencyKey(key)
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
        // save to db
        repository.save(tx);
        return paymentMapper.toResponse(tx);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID id) {
        PaymentTransaction tx = repository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return paymentMapper.toResponse(tx);
    }
}
