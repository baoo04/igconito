package com.cinema.movie.service;

import com.cinema.movie.dto.request.CreateShowtimeRequest;
import com.cinema.movie.dto.request.UpdateShowtimeRequest;
import com.cinema.movie.dto.response.ShowtimeResponse;
import com.cinema.movie.entity.Auditorium;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.entity.Showtime;
import com.cinema.movie.exception.ResourceNotFoundException;
import com.cinema.movie.mapper.ShowtimeMapper;
import com.cinema.movie.repository.AuditoriumRepository;
import com.cinema.movie.repository.MovieRepository;
import com.cinema.movie.repository.ShowtimeRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

  private final ShowtimeRepository showtimeRepository;
  private final MovieRepository movieRepository;
  private final AuditoriumRepository auditoriumRepository;
  private final ShowtimeMapper showtimeMapper;

  @Transactional(readOnly = true)
  public List<ShowtimeResponse> listByMovie(Long movieId) {
    movieRepository
        .findById(movieId)
        .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + movieId));
    return showtimeRepository.findByMovieIdOrderByStartTimeAsc(movieId).stream()
        .map(showtimeMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ShowtimeResponse> listFiltered(LocalDate date, Long cinemaId) {
    List<Showtime> rows;
    if (date != null && cinemaId != null) {
      LocalDateTime start = date.atStartOfDay();
      LocalDateTime end = date.plusDays(1).atStartOfDay();
      rows = showtimeRepository.findByAuditoriumCinemaIdAndStartTimeBetween(cinemaId, start, end);
    } else if (date != null) {
      LocalDateTime start = date.atStartOfDay();
      LocalDateTime end = date.plusDays(1).atStartOfDay();
      rows = showtimeRepository.findByStartTimeBetween(start, end);
    } else if (cinemaId != null) {
      rows = showtimeRepository.findByAuditoriumCinemaId(cinemaId);
    } else {
      rows = showtimeRepository.findAll();
    }
    return rows.stream().map(showtimeMapper::toResponse).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public ShowtimeResponse getById(Long id) {
    Showtime st =
        showtimeRepository
            .findDetailById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Showtime not found: " + id));
    return showtimeMapper.toResponse(st);
  }

  @Transactional
  public ShowtimeResponse create(CreateShowtimeRequest request) {
    validateTimes(request.getStartTime(), request.getEndTime());
    Movie movie = movieById(request.getMovieId());
    Auditorium auditorium = auditoriumById(request.getAuditoriumId());
    Showtime showtime =
        Showtime.builder()
            .movie(movie)
            .auditorium(auditorium)
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .basePrice(request.getBasePrice())
            .build();
    return showtimeMapper.toResponse(showtimeRepository.save(showtime));
  }

  @Transactional
  public ShowtimeResponse update(Long id, UpdateShowtimeRequest request) {
    Showtime showtime =
        showtimeRepository
            .findDetailById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Showtime not found: " + id));
    if (request.getMovieId() != null) {
      showtime.setMovie(movieById(request.getMovieId()));
    }
    if (request.getAuditoriumId() != null) {
      showtime.setAuditorium(auditoriumById(request.getAuditoriumId()));
    }
    LocalDateTime start = request.getStartTime() != null ? request.getStartTime() : showtime.getStartTime();
    LocalDateTime end = request.getEndTime() != null ? request.getEndTime() : showtime.getEndTime();
    validateTimes(start, end);
    if (request.getStartTime() != null) {
      showtime.setStartTime(request.getStartTime());
    }
    if (request.getEndTime() != null) {
      showtime.setEndTime(request.getEndTime());
    }
    if (request.getBasePrice() != null) {
      showtime.setBasePrice(request.getBasePrice());
    }
    if (request.getStatus() != null) {
      showtime.setStatus(request.getStatus());
    }
    return showtimeMapper.toResponse(showtime);
  }

  @Transactional
  public void delete(Long id) {
    Showtime showtime =
        showtimeRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Showtime not found: " + id));
    showtimeRepository.delete(showtime);
  }

  private void validateTimes(LocalDateTime start, LocalDateTime end) {
    if (!end.isAfter(start)) {
      throw new IllegalArgumentException("endTime must be after startTime");
    }
  }

  private Movie movieById(Long id) {
    return movieRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + id));
  }

  private Auditorium auditoriumById(Long id) {
    return auditoriumRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Auditorium not found: " + id));
  }
}
