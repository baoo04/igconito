package com.cinema.booking.service;

import com.cinema.booking.dto.BookingConfirmedResponse;
import com.cinema.booking.dto.CheckoutRequest;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.IdempotencyRecord;
import com.cinema.booking.exception.CustomerValidationFailedException;
import com.cinema.booking.exception.PaymentDeclinedException;
import com.cinema.booking.exception.SeatHoldInvalidException;
import com.cinema.booking.integration.DownstreamClients;
import com.cinema.booking.integration.dto.CustomerValidateResponseDto;
import com.cinema.booking.integration.dto.HoldResponseDto;
import com.cinema.booking.integration.dto.PaymentResponseDto;
import com.cinema.booking.integration.dto.ShowtimeResponseDto;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutService {

  private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

  private final DownstreamClients clients;
  private final BookingRepository bookingRepository;
  private final IdempotencyRecordRepository idempotencyRepository;
  private final BookingResponseMapper responseMapper;
  private final BookingWriterService bookingWriterService;
  private final ObjectMapper objectMapper;

  public BookingConfirmedResponse checkout(CheckoutRequest req, String idempotencyKey) {
    try {
      return doCheckout(req, idempotencyKey);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private BookingConfirmedResponse doCheckout(CheckoutRequest req, String idempotencyKey)
      throws IOException {
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      var recOpt = idempotencyRepository.findById(idempotencyKey.trim());
      if (recOpt.isPresent() && recOpt.get().getExpiresAt().isAfter(Instant.now())) {
        Booking b =
            bookingRepository
                .findWithTicketsById(recOpt.get().getBookingId())
                .orElseThrow(
                    () -> new IllegalStateException("Idempotency record without booking"));
        return responseMapper.toConfirmed(b);
      }
    }

    HoldResponseDto hold = clients.getHold(req.getHoldId());
    if (hold == null) {
      throw new SeatHoldInvalidException("Hold not found");
    }
    if (!"ACTIVE".equalsIgnoreCase(hold.getStatus())) {
      throw new SeatHoldInvalidException("Hold is not active");
    }
    if (hold.getExpiresAt() == null || !hold.getExpiresAt().isAfter(Instant.now())) {
      throw new SeatHoldInvalidException("Hold expired");
    }
    if (!hold.getShowtimeId().equals(req.getShowtimeId())) {
      throw new SeatHoldInvalidException("showtimeId does not match hold");
    }

    ShowtimeResponseDto showtime = clients.getShowtime(req.getShowtimeId());
    if (showtime == null) {
      throw new SeatHoldInvalidException("Showtime not found");
    }

    CustomerValidateResponseDto cv =
        clients.validateCustomer(
            req.getCustomer().getFullName(),
            req.getCustomer().getEmail(),
            req.getCustomer().getPhone());
    if (cv == null || !cv.isValid()) {
      throw new CustomerValidationFailedException(
          cv != null && cv.getErrors() != null ? cv.getErrors() : List.of("Invalid customer"));
    }

    BigDecimal sum =
        hold.getSeats().stream()
            .map(HoldResponseDto.SeatInfo::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (req.getPayment().getTotalAmount().compareTo(sum) != 0) {
      throw new IllegalArgumentException("totalAmount must equal sum of held seat prices");
    }

    String bookingCode = generateBookingCode();

    ObjectNode payBody = objectMapper.createObjectNode();
    payBody.put("bookingReference", bookingCode);
    payBody.put("amount", req.getPayment().getTotalAmount());
    payBody.put("currency", "VND");
    payBody.put("paymentMethod", req.getPayment().getMethod());
    if (req.getPayment().getCardNumber() != null) {
      payBody.put("cardNumber", req.getPayment().getCardNumber());
    }
    payBody.put("cardHolderName", req.getPayment().getCardHolderName());

    PaymentResponseDto payment = clients.createPayment(payBody);
    if (payment == null || !"SUCCESS".equals(payment.getStatus())) {
      try {
        clients.releaseHold(req.getHoldId());
      } catch (Exception ex) {
        log.warn("Release hold after payment failure failed: {}", ex.getMessage());
      }
      throw new PaymentDeclinedException("Payment was not successful");
    }

    try {
      clients.confirmHold(req.getHoldId());
    } catch (Exception ex) {
      log.error("Confirm hold failed after payment success: {}", ex.getMessage());
      throw new SeatHoldInvalidException("Could not confirm seats: " + ex.getMessage());
    }

    Booking saved =
        bookingWriterService.saveConfirmedBooking(
            req, hold, showtime, payment.getPaymentId(), bookingCode, sum);

    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      idempotencyRepository.save(
          IdempotencyRecord.builder()
              .idempotencyKey(idempotencyKey.trim())
              .bookingId(saved.getId())
              .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
              .build());
    }

    try {
      sendNotification(saved, showtime);
    } catch (Exception e) {
      log.warn("Notification failed for booking {}: {}", saved.getId(), e.getMessage());
    }

    return responseMapper.toConfirmed(
        bookingRepository.findWithTicketsById(saved.getId()).orElse(saved));
  }

  private void sendNotification(Booking booking, ShowtimeResponseDto showtime)
      throws IOException {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("bookingId", booking.getId().toString());
    root.put("bookingCode", booking.getBookingCode());
    root.put("recipientEmail", booking.getCustomerEmail());
    root.put("customerName", booking.getCustomerName());
    root.put("movieTitle", showtime.getMovieTitle());
    root.put("cinemaName", showtime.getCinemaName());
    root.put("auditoriumName", showtime.getAuditoriumName());
    root.put("startTime", showtime.getStartTime().toString());
    root.set("totalAmount", objectMapper.valueToTree(booking.getTotalAmount()));
    ArrayNode arr = root.putArray("tickets");
    for (var t : booking.getTickets()) {
      ObjectNode line = arr.addObject();
      line.put("ticketCode", t.getTicketCode());
      line.put("seatCode", t.getSeatCode());
      line.put("seatType", t.getSeatType());
      line.put("price", t.getPrice());
    }
    clients.sendNotification(root);
  }

  private static String generateBookingCode() {
    String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder sb = new StringBuilder(6);
    var r = ThreadLocalRandom.current();
    for (int i = 0; i < 6; i++) {
      sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
    }
    return "BK-" + day + "-" + sb;
  }
}
