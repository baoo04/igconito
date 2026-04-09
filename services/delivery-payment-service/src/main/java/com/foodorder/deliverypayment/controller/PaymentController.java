package com.foodorder.deliverypayment.controller;

import com.foodorder.deliverypayment.dto.MockPaymentRequest;
import com.foodorder.deliverypayment.dto.PaymentLatestPayload;
import com.foodorder.deliverypayment.dto.PaymentResponse;
import com.foodorder.deliverypayment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/mock")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse mock(@Valid @RequestBody MockPaymentRequest request) {
        return paymentService.mockPay(request);
    }

    @GetMapping("/orders/{orderId}")
    public List<PaymentResponse> listByOrder(@PathVariable Long orderId) {
        return paymentService.listByOrder(orderId);
    }

    @GetMapping("/orders/{orderId}/latest")
    public PaymentLatestPayload latest(@PathVariable Long orderId) {
        return new PaymentLatestPayload(paymentService.latestByOrder(orderId));
    }
}
