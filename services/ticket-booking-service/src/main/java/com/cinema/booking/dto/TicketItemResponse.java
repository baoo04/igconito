package com.cinema.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketItemResponse {

  private String ticketCode;
  private String seatCode;
  private String seatType;
  private BigDecimal price;
  private String movieTitle;
  private LocalDateTime startTime;
}
