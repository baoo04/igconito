package com.cinema.movie.controller;

import com.cinema.movie.dto.request.CreateCinemaRequest;
import com.cinema.movie.dto.response.ApiResponse;
import com.cinema.movie.dto.response.CinemaResponse;
import com.cinema.movie.service.CinemaService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/cinemas")
@RequiredArgsConstructor
public class CinemaController {

  private final CinemaService cinemaService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<CinemaResponse>>> list() {
    return ResponseEntity.ok(ApiResponse.ok(cinemaService.listAll()));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<CinemaResponse>> create(
      @Valid @RequestBody CreateCinemaRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created(cinemaService.create(request)));
  }

  @GetMapping("/{cinemaId}")
  public ResponseEntity<ApiResponse<CinemaResponse>> getById(@PathVariable Long cinemaId) {
    return ResponseEntity.ok(ApiResponse.ok(cinemaService.getById(cinemaId)));
  }
}
