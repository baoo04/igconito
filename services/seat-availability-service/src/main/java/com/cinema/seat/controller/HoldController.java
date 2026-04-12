package com.cinema.seat.controller;

import com.cinema.seat.dto.request.CreateHoldRequest;
import com.cinema.seat.dto.response.ApiResponse;
import com.cinema.seat.dto.response.HoldResponse;
import com.cinema.seat.service.HoldService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HoldController {

  private final HoldService holdService;

  @PostMapping("/holds")
  public ResponseEntity<ApiResponse<HoldResponse>> create(@Valid @RequestBody CreateHoldRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created(holdService.createHold(request)));
  }

  @GetMapping("/holds/{holdId}")
  public ResponseEntity<ApiResponse<HoldResponse>> get(@PathVariable UUID holdId) {
    return ResponseEntity.ok(ApiResponse.ok(holdService.getHold(holdId)));
  }

  @PostMapping("/holds/{holdId}/release")
  public ResponseEntity<ApiResponse<Map<String, String>>> release(@PathVariable UUID holdId) {
    holdService.releaseHold(holdId);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("result", "released")));
  }

  @PostMapping("/holds/{holdId}/confirm")
  public ResponseEntity<ApiResponse<Map<String, String>>> confirm(@PathVariable UUID holdId) {
    holdService.confirmHold(holdId);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("result", "confirmed")));
  }
}
