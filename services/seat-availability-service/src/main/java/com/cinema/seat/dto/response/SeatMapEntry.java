package com.cinema.seat.dto.response;

import com.cinema.seat.entity.SeatStatus;
import com.cinema.seat.entity.SeatType;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SeatMapEntry {

  private Long seatId;
  private String seatCode;
  private SeatStatus status;
  private SeatType type;
  private BigDecimal price;
}
