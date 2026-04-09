package com.foodorder.deliverypayment.dto;

import com.foodorder.deliverypayment.entity.DeliveryStatus;
import java.time.Instant;

public record DeliveryResponse(Long id, Long orderId, DeliveryStatus status, String trackingNumber, Instant updatedAt) {}
