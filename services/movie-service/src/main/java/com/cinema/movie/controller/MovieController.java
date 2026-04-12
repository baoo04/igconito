package com.cinema.movie.controller;

import com.cinema.movie.dto.request.CreateMovieRequest;
import com.cinema.movie.dto.request.UpdateMovieRequest;
import com.cinema.movie.dto.response.ApiResponse;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.dto.response.ShowtimeResponse;
import com.cinema.movie.entity.MovieStatus;
import com.cinema.movie.service.MovieService;
import com.cinema.movie.service.ShowtimeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

  private final MovieService movieService;
  private final ShowtimeService showtimeService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<MovieResponse>>> list(
      @RequestParam(required = false) MovieStatus status,
      @RequestParam(required = false) String genre) {
    return ResponseEntity.ok(ApiResponse.ok(movieService.list(status, genre)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<MovieResponse>> create(
      @Valid @RequestBody CreateMovieRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created(movieService.create(request)));
  }

  @GetMapping("/{movieId}")
  public ResponseEntity<ApiResponse<MovieResponse>> getById(@PathVariable Long movieId) {
    return ResponseEntity.ok(ApiResponse.ok(movieService.getById(movieId)));
  }

  @PutMapping("/{movieId}")
  public ResponseEntity<ApiResponse<MovieResponse>> update(
      @PathVariable Long movieId, @Valid @RequestBody UpdateMovieRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(movieService.update(movieId, request)));
  }

  @DeleteMapping("/{movieId}")
  public ResponseEntity<Void> delete(@PathVariable Long movieId) {
    movieService.delete(movieId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{movieId}/showtimes")
  public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> showtimesForMovie(
      @PathVariable Long movieId) {
    return ResponseEntity.ok(ApiResponse.ok(showtimeService.listByMovie(movieId)));
  }
}
