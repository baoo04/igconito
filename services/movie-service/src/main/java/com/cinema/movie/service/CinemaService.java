package com.cinema.movie.service;

import com.cinema.movie.dto.request.CreateCinemaRequest;
import com.cinema.movie.dto.response.CinemaResponse;
import com.cinema.movie.entity.Cinema;
import com.cinema.movie.exception.ResourceNotFoundException;
import com.cinema.movie.mapper.CinemaMapper;
import com.cinema.movie.repository.CinemaRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CinemaService {

  private final CinemaRepository cinemaRepository;
  private final CinemaMapper cinemaMapper;

  @Transactional(readOnly = true)
  public List<CinemaResponse> listAll() {
    return cinemaRepository.findAll().stream()
        .map(cinemaMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public CinemaResponse getById(Long id) {
    return cinemaMapper.toResponse(cinemaById(id));
  }

  @Transactional
  public CinemaResponse create(CreateCinemaRequest request) {
    Cinema cinema = cinemaMapper.toEntity(request);
    return cinemaMapper.toResponse(cinemaRepository.save(cinema));
  }

  private Cinema cinemaById(Long id) {
    return cinemaRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Cinema not found: " + id));
  }
}
