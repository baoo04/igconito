package com.cinema.booking.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShowtimeResponseDto {

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
  private String status;
}
