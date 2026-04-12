package com.cinema.movie.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CreateMovieRequest {

  @NotBlank private String title;
  private String description;
  private String genre;

  @Positive private Integer durationMinutes;

  private Double rating;
  private String posterUrl;
  private LocalDate releaseDate;
}
