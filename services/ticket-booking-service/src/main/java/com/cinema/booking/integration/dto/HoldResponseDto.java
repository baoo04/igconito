package com.cinema.booking.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HoldResponseDto {

  private UUID holdId;
  private Long showtimeId;
  private String status;
  private Instant expiresAt;
  private List<SeatInfo> seats;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SeatInfo {
    private Long seatId;
    private String seatCode;
    private String rowLabel;
    private Integer seatNumber;
    private String type;
    private BigDecimal price;
  }
}
