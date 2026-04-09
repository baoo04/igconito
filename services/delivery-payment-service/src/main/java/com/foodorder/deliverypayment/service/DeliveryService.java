package com.foodorder.deliverypayment.service;

import com.foodorder.deliverypayment.dto.DeliveryResponse;
import com.foodorder.deliverypayment.dto.DeliveryStatusUpdateRequest;
import com.foodorder.deliverypayment.entity.Delivery;
import com.foodorder.deliverypayment.entity.DeliveryStatus;
import com.foodorder.deliverypayment.repository.DeliveryRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Transactional
    public DeliveryResponse start(Long orderId) {
        if (deliveryRepository.findByOrderId(orderId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delivery already exists for order");
        }
        Delivery d = Delivery.builder()
                .orderId(orderId)
                .status(DeliveryStatus.PENDING)
                .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .updatedAt(Instant.now())
                .build();
        return toResponse(deliveryRepository.save(d));
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getByOrder(Long orderId) {
        return deliveryRepository
                .findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
    }

    @Transactional(readOnly = true)
    public DeliveryResponse findByOrderOrNull(Long orderId) {
        return deliveryRepository.findByOrderId(orderId).map(this::toResponse).orElse(null);
    }

    @Transactional
    public DeliveryResponse updateStatus(Long deliveryId, DeliveryStatusUpdateRequest request) {
        Delivery d = deliveryRepository
                .findById(deliveryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
        d.setStatus(request.status());
        d.setUpdatedAt(Instant.now());
        return toResponse(deliveryRepository.save(d));
    }

    private DeliveryResponse toResponse(Delivery d) {
        return new DeliveryResponse(d.getId(), d.getOrderId(), d.getStatus(), d.getTrackingNumber(), d.getUpdatedAt());
    }
}
