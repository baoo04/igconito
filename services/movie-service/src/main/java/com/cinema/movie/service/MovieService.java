package com.cinema.movie.service;

import com.cinema.movie.dto.request.CreateMovieRequest;
import com.cinema.movie.dto.request.UpdateMovieRequest;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.entity.MovieStatus;
import com.cinema.movie.exception.ResourceNotFoundException;
import com.cinema.movie.mapper.MovieMapper;
import com.cinema.movie.repository.MovieRepository;
import com.cinema.movie.repository.ShowtimeRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovieService {

  private final MovieRepository movieRepository;
  private final ShowtimeRepository showtimeRepository;
  private final MovieMapper movieMapper;

  @Transactional(readOnly = true)
  public List<MovieResponse> list(MovieStatus status, String genre) {
    List<Movie> movies;
    if (status != null && genre != null && !genre.isBlank()) {
      movies = movieRepository.findByStatusAndGenreIgnoreCase(status, genre);
    } else if (status != null) {
      movies = movieRepository.findByStatus(status);
    } else if (genre != null && !genre.isBlank()) {
      movies = movieRepository.findByGenreIgnoreCase(genre);
    } else {
      movies = movieRepository.findAll();
    }
    return movies.stream().map(movieMapper::toResponse).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public MovieResponse getById(Long id) {
    return movieMapper.toResponse(movieById(id));
  }

  @Transactional
  public MovieResponse create(CreateMovieRequest request) {
    Movie movie = movieMapper.toEntity(request);
    movie.setStatus(MovieStatus.ACTIVE);
    return movieMapper.toResponse(movieRepository.save(movie));
  }

  @Transactional
  public MovieResponse update(Long id, UpdateMovieRequest request) {
    Movie movie = movieById(id);
    if (request.getTitle() != null) {
      movie.setTitle(request.getTitle());
    }
    if (request.getDescription() != null) {
      movie.setDescription(request.getDescription());
    }
    if (request.getGenre() != null) {
      movie.setGenre(request.getGenre());
    }
    if (request.getDurationMinutes() != null) {
      movie.setDurationMinutes(request.getDurationMinutes());
    }
    if (request.getRating() != null) {
      movie.setRating(request.getRating());
    }
    if (request.getPosterUrl() != null) {
      movie.setPosterUrl(request.getPosterUrl());
    }
    if (request.getReleaseDate() != null) {
      movie.setReleaseDate(request.getReleaseDate());
    }
    if (request.getStatus() != null) {
      movie.setStatus(request.getStatus());
    }
    return movieMapper.toResponse(movie);
  }

  @Transactional
  public void delete(Long id) {
    Movie movie = movieById(id);
    if (!showtimeRepository.findByMovieIdOrderByStartTimeAsc(id).isEmpty()) {
      throw new IllegalArgumentException("Cannot delete movie with existing showtimes");
    }
    movieRepository.delete(movie);
  }

  private Movie movieById(Long id) {
    return movieRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + id));
  }
}
