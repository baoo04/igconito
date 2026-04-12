package com.cinema.movie.dto.request;

import com.cinema.movie.entity.MovieStatus;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.Data;

@Data
public class UpdateMovieRequest {

  private String title;
  private String description;
  private String genre;

  @Positive private Integer durationMinutes;

  private Double rating;
  private String posterUrl;
  private LocalDate releaseDate;
  private MovieStatus status;
}
