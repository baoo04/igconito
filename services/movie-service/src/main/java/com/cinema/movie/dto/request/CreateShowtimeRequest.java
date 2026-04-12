package com.cinema.movie.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CreateShowtimeRequest {

  @NotNull @Positive private Long movieId;
  @NotNull @Positive private Long auditoriumId;

  @NotNull private LocalDateTime startTime;
  @NotNull private LocalDateTime endTime;

  @NotNull private BigDecimal basePrice;
}
