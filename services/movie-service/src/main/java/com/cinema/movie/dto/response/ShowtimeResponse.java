package com.cinema.movie.dto.response;

import com.cinema.movie.entity.ShowtimeStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShowtimeResponse {

  private Long id;
  private Long movieId;
  private String movieTitle;
  private Long cinemaId;
  private String cinemaName;
  private Long auditoriumId;
  private String auditoriumName;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private BigDecimal basePrice;
  private ShowtimeStatus status;
}
