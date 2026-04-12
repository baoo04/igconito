package com.cinema.seat.controller;

import com.cinema.seat.dto.request.CheckAvailabilityRequest;
import com.cinema.seat.dto.response.ApiResponse;
import com.cinema.seat.dto.response.SeatMapResponse;
import com.cinema.seat.service.HoldService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShowtimeSeatController {

  private final HoldService holdService;

  @GetMapping("/showtimes/{showtimeId}/seats")
  public ResponseEntity<ApiResponse<SeatMapResponse>> seatMap(@PathVariable Long showtimeId) {
    return ResponseEntity.ok(ApiResponse.ok(holdService.getSeatMap(showtimeId)));
  }

  @PostMapping("/showtimes/{showtimeId}/availability")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkAvailability(
      @PathVariable Long showtimeId, @Valid @RequestBody CheckAvailabilityRequest request) {
    holdService.checkAvailability(showtimeId, request);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("available", true)));
  }
}
