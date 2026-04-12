package com.cinema.seat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "seats",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"showtime_id", "seat_code"}),
    indexes = @Index(name = "idx_seats_showtime", columnList = "showtime_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "showtime_id", nullable = false)
  private Long showtimeId;

  @Column(name = "row_label", nullable = false, length = 8)
  private String rowLabel;

  @Column(name = "seat_number", nullable = false)
  private Integer seatNumber;

  @Column(name = "seat_code", nullable = false, length = 16)
  private String seatCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private SeatType type = SeatType.STANDARD;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private SeatStatus status = SeatStatus.FREE;
}
