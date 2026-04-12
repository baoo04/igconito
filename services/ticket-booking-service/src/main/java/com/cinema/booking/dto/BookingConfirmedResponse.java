package com.cinema.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingConfirmedResponse {

  private UUID bookingId;
  private String bookingCode;
  private String status;
  private Customer customer;
  private Showtime showtime;
  private List<TicketLine> tickets;
  private BigDecimal totalAmount;
  private UUID paymentId;
  private Instant createdAt;

  @Data
  @Builder
  public static class Customer {
    private String fullName;
    private String email;
    private String phone;
  }

  @Data
  @Builder
  public static class Showtime {
    private Long showtimeId;
    private String movieTitle;
    private LocalDateTime startTime;
    private String cinemaName;
    private String auditoriumName;
  }

  @Data
  @Builder
  public static class TicketLine {
    private String ticketCode;
    private String seatCode;
    private String seatType;
    private BigDecimal price;
  }
}
