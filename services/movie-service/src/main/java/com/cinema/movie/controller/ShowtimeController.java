package com.cinema.movie.controller;

import com.cinema.movie.dto.request.CreateShowtimeRequest;
import com.cinema.movie.dto.request.UpdateShowtimeRequest;
import com.cinema.movie.dto.response.ApiResponse;
import com.cinema.movie.dto.response.ShowtimeResponse;
import com.cinema.movie.service.ShowtimeService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

  private final ShowtimeService showtimeService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> list(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) Long cinemaId) {
    return ResponseEntity.ok(ApiResponse.ok(showtimeService.listFiltered(date, cinemaId)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<ShowtimeResponse>> create(
      @Valid @RequestBody CreateShowtimeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created(showtimeService.create(request)));
  }

  @GetMapping("/{showtimeId}")
  public ResponseEntity<ApiResponse<ShowtimeResponse>> getById(@PathVariable Long showtimeId) {
    return ResponseEntity.ok(ApiResponse.ok(showtimeService.getById(showtimeId)));
  }

  @PutMapping("/{showtimeId}")
  public ResponseEntity<ApiResponse<ShowtimeResponse>> update(
      @PathVariable Long showtimeId, @Valid @RequestBody UpdateShowtimeRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(showtimeService.update(showtimeId, request)));
  }

  @DeleteMapping("/{showtimeId}")
  public ResponseEntity<Void> delete(@PathVariable Long showtimeId) {
    showtimeService.delete(showtimeId);
    return ResponseEntity.noContent().build();
  }
}
