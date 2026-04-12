package com.cinema.movie.dto.request;

import com.cinema.movie.entity.ShowtimeStatus;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UpdateShowtimeRequest {

  @Positive private Long movieId;
  @Positive private Long auditoriumId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private BigDecimal basePrice;
  private ShowtimeStatus status;
}
