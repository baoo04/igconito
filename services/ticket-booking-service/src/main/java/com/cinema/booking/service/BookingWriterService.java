package com.cinema.booking.service;

import com.cinema.booking.dto.CheckoutRequest;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingStatus;
import com.cinema.booking.entity.Ticket;
import com.cinema.booking.integration.dto.HoldResponseDto;
import com.cinema.booking.integration.dto.ShowtimeResponseDto;
import com.cinema.booking.repository.BookingRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingWriterService {

  private final BookingRepository bookingRepository;

  @Transactional
  public Booking saveConfirmedBooking(
      CheckoutRequest req,
      HoldResponseDto hold,
      ShowtimeResponseDto showtime,
      UUID paymentId,
      String bookingCode,
      BigDecimal total) {
    Booking b =
        Booking.builder()
            .bookingCode(bookingCode)
            .status(BookingStatus.CONFIRMED)
            .showtimeId(req.getShowtimeId())
            .customerEmail(req.getCustomer().getEmail())
            .customerName(req.getCustomer().getFullName())
            .customerPhone(req.getCustomer().getPhone())
            .totalAmount(total)
            .paymentId(paymentId)
            .holdId(req.getHoldId())
            .tickets(new ArrayList<>())
            .build();

    for (HoldResponseDto.SeatInfo s : hold.getSeats()) {
      Ticket t =
          Ticket.builder()
              .booking(b)
              .seatId(s.getSeatId())
              .seatCode(s.getSeatCode())
              .seatType(s.getType())
              .price(s.getPrice())
              .ticketCode(
                  "TKT-"
                      + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase())
              .showtimeId(req.getShowtimeId())
              .movieTitle(showtime.getMovieTitle())
              .startTime(showtime.getStartTime())
              .cinemaName(showtime.getCinemaName())
              .auditoriumName(showtime.getAuditoriumName())
              .build();
      b.getTickets().add(t);
    }
    return bookingRepository.save(b);
  }
}
