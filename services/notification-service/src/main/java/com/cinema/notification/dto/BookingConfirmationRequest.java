package com.cinema.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class BookingConfirmationRequest {

  @NotNull private UUID bookingId;

  @NotBlank private String bookingCode;

  @NotBlank @Email private String recipientEmail;

  @NotBlank private String customerName;

  @NotBlank private String movieTitle;

  @NotBlank private String cinemaName;

  @NotBlank private String auditoriumName;

  @NotNull private LocalDateTime startTime;

  @Valid @NotNull private List<TicketLine> tickets;

  @NotNull private BigDecimal totalAmount;

  @Data
  public static class TicketLine {
    @NotBlank private String ticketCode;
    @NotBlank private String seatCode;
    @NotBlank private String seatType;
    @NotNull private BigDecimal price;
  }
}
