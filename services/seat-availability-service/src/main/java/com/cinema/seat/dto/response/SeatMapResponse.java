package com.cinema.seat.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SeatMapResponse {

  private Long showtimeId;
  private List<SeatMapEntry> seats;
}
