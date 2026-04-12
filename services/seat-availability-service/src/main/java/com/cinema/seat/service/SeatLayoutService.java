package com.cinema.seat.service;

import com.cinema.seat.dto.response.SeatMapEntry;
import com.cinema.seat.dto.response.SeatMapResponse;
import com.cinema.seat.entity.Seat;
import com.cinema.seat.entity.SeatStatus;
import com.cinema.seat.entity.SeatType;
import com.cinema.seat.repository.SeatRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatLayoutService {

  private final SeatRepository seatRepository;

  @Value("${cinema.seat.default-price}")
  private BigDecimal defaultPrice;

  @Value("${cinema.seat.rows}")
  private int layoutRows;

  @Value("${cinema.seat.seats-per-row}")
  private int seatsPerRow;

  /** Không dùng readOnly — {@link #loadOrCreateSeats} có thể INSERT khi chưa có ghế cho suất. */
  @Transactional
  public SeatMapResponse buildMap(Long showtimeId) {
    List<Seat> seats = loadOrCreateSeats(showtimeId);
    return SeatMapResponse.builder()
        .showtimeId(showtimeId)
        .seats(seats.stream().map(this::toEntry).collect(Collectors.toList()))
        .build();
  }

  @Transactional
  public List<Seat> loadOrCreateSeats(Long showtimeId) {
    if (seatRepository.countByShowtimeId(showtimeId) > 0) {
      return seatRepository.findByShowtimeIdOrderByRowLabelAscSeatNumberAsc(showtimeId);
    }
    try {
      return seatRepository.saveAll(buildGrid(showtimeId));
    } catch (DataIntegrityViolationException ex) {
      return seatRepository.findByShowtimeIdOrderByRowLabelAscSeatNumberAsc(showtimeId);
    }
  }

  private List<Seat> buildGrid(Long showtimeId) {
    List<Seat> out = new ArrayList<>();
    for (int r = 0; r < layoutRows; r++) {
      String row = String.valueOf((char) ('A' + r));
      SeatType type = r >= layoutRows - 2 ? SeatType.VIP : SeatType.STANDARD;
      BigDecimal price =
          type == SeatType.VIP
              ? defaultPrice.multiply(BigDecimal.valueOf(1.5)).setScale(2, RoundingMode.HALF_UP)
              : defaultPrice.setScale(2, RoundingMode.HALF_UP);
      for (int n = 1; n <= seatsPerRow; n++) {
        String code = row + n;
        out.add(
            Seat.builder()
                .showtimeId(showtimeId)
                .rowLabel(row)
                .seatNumber(n)
                .seatCode(code)
                .type(type)
                .price(price)
                .status(SeatStatus.FREE)
                .build());
      }
    }
    return out;
  }

  private SeatMapEntry toEntry(Seat s) {
    return SeatMapEntry.builder()
        .seatId(s.getId())
        .seatCode(s.getSeatCode())
        .status(s.getStatus())
        .type(s.getType())
        .price(s.getPrice())
        .build();
  }
}
