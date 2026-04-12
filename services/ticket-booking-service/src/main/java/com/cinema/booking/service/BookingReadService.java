package com.cinema.booking.service;

import com.cinema.booking.dto.BookingConfirmedResponse;
import com.cinema.booking.dto.TicketItemResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingStatus;
import com.cinema.booking.exception.ConflictException;
import com.cinema.booking.exception.ResourceNotFoundException;
import com.cinema.booking.repository.BookingRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingReadService {

  private final BookingRepository bookingRepository;
  private final BookingResponseMapper responseMapper;

  @Transactional(readOnly = true)
  public BookingConfirmedResponse getBooking(UUID id) {
    Booking b =
        bookingRepository
            .findWithTicketsById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    return responseMapper.toConfirmed(b);
  }

  @Transactional(readOnly = true)
  public List<TicketItemResponse> listTickets(UUID bookingId) {
    Booking b =
        bookingRepository
            .findWithTicketsById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    return b.getTickets().stream()
        .map(
            t ->
                TicketItemResponse.builder()
                    .ticketCode(t.getTicketCode())
                    .seatCode(t.getSeatCode())
                    .seatType(t.getSeatType())
                    .price(t.getPrice())
                    .movieTitle(t.getMovieTitle())
                    .startTime(t.getStartTime())
                    .build())
        .collect(Collectors.toList());
  }

  @Transactional
  public void cancel(UUID bookingId) {
    Booking b =
        bookingRepository
            .findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    if (b.getStatus() == BookingStatus.CANCELLED) {
      throw new ConflictException("Booking already cancelled");
    }
    if (b.getStatus() != BookingStatus.CONFIRMED) {
      throw new ConflictException("Cannot cancel booking in state: " + b.getStatus());
    }
    b.setStatus(BookingStatus.CANCELLED);
  }
}
