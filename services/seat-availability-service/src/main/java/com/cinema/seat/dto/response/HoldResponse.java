package com.cinema.seat.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HoldResponse {

  private UUID holdId;
  private Long showtimeId;
  private String status;
  /** Thời điểm hết hạn (UTC, ISO-8601 có hậu tố Z) — tránh lệch múi giờ với trình duyệt. */
  private Instant expiresAt;
  private List<SeatInfo> seats;

  @Data
  @Builder
  public static class SeatInfo {
    private Long seatId;
    private String seatCode;
    private String rowLabel;
    private Integer seatNumber;
    private String type;
    private java.math.BigDecimal price;
  }
}
