package com.cinema.seat.client;

import com.cinema.seat.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class MovieCatalogClient {

  private final RestClient movieRestClient;

  /** Ensures the showtime exists in movie-service; throws ResourceNotFoundException if 404. */
  public void requireShowtimeExists(Long showtimeId) {
    try {
      movieRestClient.get().uri("/showtimes/{id}", showtimeId).retrieve().toBodilessEntity();
    } catch (RestClientResponseException e) {
      if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
        throw new ResourceNotFoundException("Showtime not found: " + showtimeId);
      }
      throw new IllegalStateException("Movie service error: HTTP " + e.getStatusCode().value(), e);
    } catch (RestClientException e) {
      throw new IllegalStateException("Movie service unavailable: " + e.getMessage(), e);
    }
  }
}
