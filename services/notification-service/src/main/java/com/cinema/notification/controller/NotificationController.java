package com.cinema.notification.controller;

import com.cinema.notification.dto.ApiResponse;
import com.cinema.notification.dto.BookingConfirmationRequest;
import com.cinema.notification.dto.NotificationResponse;
import com.cinema.notification.service.NotificationDispatchService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationDispatchService dispatchService;

  @PostMapping("/booking-confirmations")
  public ResponseEntity<ApiResponse<NotificationResponse>> bookingConfirmation(
      @Valid @RequestBody BookingConfirmationRequest request) {
    NotificationResponse body = dispatchService.sendBookingConfirmation(request);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.accepted(body));
  }

  @GetMapping("/{notificationId}")
  public ResponseEntity<ApiResponse<NotificationResponse>> get(@PathVariable UUID notificationId) {
    return ResponseEntity.ok(ApiResponse.ok(dispatchService.get(notificationId)));
  }
}
