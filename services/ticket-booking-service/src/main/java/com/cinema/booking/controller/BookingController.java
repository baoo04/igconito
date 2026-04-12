package com.cinema.booking.controller;

import com.cinema.booking.dto.ApiResponse;
import com.cinema.booking.dto.BookingConfirmedResponse;
import com.cinema.booking.dto.CheckoutRequest;
import com.cinema.booking.dto.TicketItemResponse;
import com.cinema.booking.service.BookingReadService;
import com.cinema.booking.service.CheckoutService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

  private final CheckoutService checkoutService;
  private final BookingReadService bookingReadService;

  @PostMapping("/checkout")
  public ResponseEntity<ApiResponse<BookingConfirmedResponse>> checkout(
      @Valid @RequestBody CheckoutRequest request,
      @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created(checkoutService.checkout(request, idempotencyKey)));
  }

  @GetMapping("/{bookingId}")
  public ResponseEntity<ApiResponse<BookingConfirmedResponse>> get(@PathVariable UUID bookingId) {
    return ResponseEntity.ok(ApiResponse.ok(bookingReadService.getBooking(bookingId)));
  }

  @GetMapping("/{bookingId}/tickets")
  public ResponseEntity<ApiResponse<List<TicketItemResponse>>> tickets(@PathVariable UUID bookingId) {
    return ResponseEntity.ok(ApiResponse.ok(bookingReadService.listTickets(bookingId)));
  }

  @PostMapping("/{bookingId}/cancel")
  public ResponseEntity<ApiResponse<Map<String, String>>> cancel(@PathVariable UUID bookingId) {
    bookingReadService.cancel(bookingId);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "CANCELLED")));
  }
}
