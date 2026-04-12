package com.cinema.notification.service;

import com.cinema.notification.dto.BookingConfirmationRequest;
import com.cinema.notification.dto.NotificationResponse;
import com.cinema.notification.entity.NotificationLog;
import com.cinema.notification.entity.NotificationStatus;
import com.cinema.notification.exception.ResourceNotFoundException;
import com.cinema.notification.repository.NotificationLogRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

  private final NotificationLogRepository repository;
  private final MockEmailSender emailSender;

  @Transactional
  public NotificationResponse sendBookingConfirmation(BookingConfirmationRequest req) {
    String subject = "Booking confirmed: " + req.getBookingCode() + " — " + req.getMovieTitle();
    String body = buildBody(req);

    NotificationLog log =
        NotificationLog.builder()
            .bookingId(req.getBookingId())
            .recipientEmail(req.getRecipientEmail())
            .subject(subject)
            .status(NotificationStatus.PENDING)
            .attemptCount(0)
            .build();
    log = repository.save(log);

    try {
      log.setAttemptCount(1);
      log.setLastAttemptAt(LocalDateTime.now());
      emailSender.send(req.getRecipientEmail(), subject, body);
      log.setStatus(NotificationStatus.SENT);
      log.setSentAt(LocalDateTime.now());
      repository.save(log);
      return NotificationResponse.builder()
          .notificationId(log.getId())
          .status(NotificationStatus.SENT.name())
          .message("Notification queued (mock sender)")
          .build();
    } catch (Exception e) {
      log.setStatus(NotificationStatus.FAILED);
      log.setErrorMessage(e.getMessage());
      repository.save(log);
      throw e;
    }
  }

  @Transactional(readOnly = true)
  public NotificationResponse get(UUID id) {
    NotificationLog log =
        repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
    return NotificationResponse.builder()
        .notificationId(log.getId())
        .status(log.getStatus().name())
        .message(log.getSubject())
        .build();
  }

  private static String buildBody(BookingConfirmationRequest req) {
    String lines =
        req.getTickets().stream()
            .map(t -> t.getSeatCode() + " (" + t.getTicketCode() + ") — " + t.getPrice() + " VND")
            .collect(Collectors.joining("\n"));
    return "Hello "
        + req.getCustomerName()
        + ",\n\nYour booking "
        + req.getBookingCode()
        + " is confirmed.\n"
        + "Movie: "
        + req.getMovieTitle()
        + "\n"
        + "Where: "
        + req.getCinemaName()
        + ", "
        + req.getAuditoriumName()
        + "\n"
        + "Time: "
        + req.getStartTime()
        + "\n\nSeats:\n"
        + lines
        + "\n\nTotal: "
        + req.getTotalAmount()
        + " VND\n";
  }
}
