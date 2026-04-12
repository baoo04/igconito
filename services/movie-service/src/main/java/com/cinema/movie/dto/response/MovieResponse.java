package com.cinema.movie.dto.response;

import com.cinema.movie.entity.MovieStatus;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MovieResponse {

  private Long id;
  private String title;
  private String description;
  private String genre;
  private Integer durationMinutes;
  private Double rating;
  private String posterUrl;
  private LocalDate releaseDate;
  private MovieStatus status;
}
