package com.cinema.booking.service;

import com.cinema.booking.dto.BookingConfirmedResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Ticket;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BookingResponseMapper {

  public BookingConfirmedResponse toConfirmed(Booking b) {
    return BookingConfirmedResponse.builder()
        .bookingId(b.getId())
        .bookingCode(b.getBookingCode())
        .status(b.getStatus().name())
        .customer(
            BookingConfirmedResponse.Customer.builder()
                .fullName(b.getCustomerName())
                .email(b.getCustomerEmail())
                .phone(b.getCustomerPhone())
                .build())
        .showtime(
            BookingConfirmedResponse.Showtime.builder()
                .showtimeId(b.getShowtimeId())
                .movieTitle(firstTicketMovieTitle(b))
                .startTime(firstTicketStart(b))
                .cinemaName(firstTicketCinema(b))
                .auditoriumName(firstTicketAuditorium(b))
                .build())
        .tickets(
            b.getTickets().stream()
                .map(
                    t ->
                        BookingConfirmedResponse.TicketLine.builder()
                            .ticketCode(t.getTicketCode())
                            .seatCode(t.getSeatCode())
                            .seatType(t.getSeatType())
                            .price(t.getPrice())
                            .build())
                .collect(Collectors.toList()))
        .totalAmount(b.getTotalAmount())
        .paymentId(b.getPaymentId())
        .createdAt(b.getCreatedAt())
        .build();
  }

  private static String firstTicketMovieTitle(Booking b) {
    return b.getTickets().isEmpty() ? "" : b.getTickets().get(0).getMovieTitle();
  }

  private static java.time.LocalDateTime firstTicketStart(Booking b) {
    return b.getTickets().isEmpty() ? null : b.getTickets().get(0).getStartTime();
  }

  private static String firstTicketCinema(Booking b) {
    return b.getTickets().isEmpty() ? "" : b.getTickets().get(0).getCinemaName();
  }

  private static String firstTicketAuditorium(Booking b) {
    return b.getTickets().isEmpty() ? "" : b.getTickets().get(0).getAuditoriumName();
  }
}
