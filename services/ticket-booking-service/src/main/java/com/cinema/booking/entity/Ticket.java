package com.cinema.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @Column(name = "seat_id", nullable = false)
  private Long seatId;

  @Column(name = "seat_code", nullable = false)
  private String seatCode;

  @Column(name = "seat_type", nullable = false)
  private String seatType;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal price;

  @Column(name = "ticket_code", nullable = false, unique = true, length = 40)
  private String ticketCode;

  @Column(name = "showtime_id", nullable = false)
  private Long showtimeId;

  @Column(name = "movie_title", nullable = false)
  private String movieTitle;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "cinema_name", nullable = false)
  private String cinemaName;

  @Column(name = "auditorium_name", nullable = false)
  private String auditoriumName;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }
}
