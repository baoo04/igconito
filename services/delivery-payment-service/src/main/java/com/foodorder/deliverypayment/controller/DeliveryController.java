package com.foodorder.deliverypayment.controller;

import com.foodorder.deliverypayment.dto.DeliveryLookupPayload;
import com.foodorder.deliverypayment.dto.DeliveryResponse;
import com.foodorder.deliverypayment.dto.DeliveryStatusUpdateRequest;
import com.foodorder.deliverypayment.dto.StartDeliveryRequest;
import com.foodorder.deliverypayment.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryResponse start(@Valid @RequestBody StartDeliveryRequest request) {
        return deliveryService.start(request.orderId());
    }

    @GetMapping("/orders/{orderId}")
    public DeliveryLookupPayload byOrder(@PathVariable Long orderId) {
        return new DeliveryLookupPayload(deliveryService.findByOrderOrNull(orderId));
    }

    @PatchMapping("/{id}/status")
    public DeliveryResponse status(@PathVariable Long id, @Valid @RequestBody DeliveryStatusUpdateRequest request) {
        return deliveryService.updateStatus(id, request);
    }
}
