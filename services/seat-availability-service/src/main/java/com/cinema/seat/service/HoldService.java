package com.cinema.seat.service;

import com.cinema.seat.client.MovieCatalogClient;
import com.cinema.seat.dto.request.CheckAvailabilityRequest;
import com.cinema.seat.dto.request.CreateHoldRequest;
import com.cinema.seat.dto.response.HoldResponse;
import com.cinema.seat.dto.response.SeatMapResponse;
import com.cinema.seat.entity.Seat;
import com.cinema.seat.entity.SeatHold;
import com.cinema.seat.entity.SeatHoldItem;
import com.cinema.seat.entity.SeatHoldStatus;
import com.cinema.seat.entity.SeatStatus;
import com.cinema.seat.exception.HoldConflictException;
import com.cinema.seat.exception.ResourceNotFoundException;
import com.cinema.seat.exception.SeatConflictException;
import com.cinema.seat.repository.SeatHoldRepository;
import com.cinema.seat.repository.SeatRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HoldService {

  private final MovieCatalogClient movieCatalogClient;
  private final SeatLayoutService seatLayoutService;
  private final SeatRepository seatRepository;
  private final SeatHoldRepository seatHoldRepository;

  @Value("${cinema.hold.ttl-minutes}")
  private int holdTtlMinutes;

  public SeatMapResponse getSeatMap(Long showtimeId) {
    movieCatalogClient.requireShowtimeExists(showtimeId);
    return seatLayoutService.buildMap(showtimeId);
  }

  /** Không readOnly — có thể tạo bản ghi ghế lần đầu qua {@link SeatLayoutService#loadOrCreateSeats}. */
  @Transactional
  public void checkAvailability(Long showtimeId, CheckAvailabilityRequest request) {
    movieCatalogClient.requireShowtimeExists(showtimeId);
    seatLayoutService.loadOrCreateSeats(showtimeId);
    List<Long> ids = request.getSeatIds().stream().distinct().toList();
    List<Seat> seats = loadSeatsForShowtime(showtimeId, ids);
    for (Seat seat : seats) {
      if (seat.getStatus() != SeatStatus.FREE) {
        throw new SeatConflictException("Seat not available: " + seat.getSeatCode());
      }
    }
  }

  @Transactional
  public HoldResponse createHold(CreateHoldRequest request) {
    movieCatalogClient.requireShowtimeExists(request.getShowtimeId());
    seatLayoutService.loadOrCreateSeats(request.getShowtimeId());

    List<Long> sortedIds =
        request.getSeatIds().stream().distinct().sorted().toList();
    if (sortedIds.isEmpty()) {
      throw new IllegalArgumentException("seatIds must not be empty");
    }

    List<Seat> locked = seatRepository.lockByIds(sortedIds);
    if (locked.size() != sortedIds.size()) {
      throw new SeatConflictException("One or more seats not found");
    }
    for (Seat seat : locked) {
      if (!seat.getShowtimeId().equals(request.getShowtimeId())) {
        throw new SeatConflictException("Seat " + seat.getSeatCode() + " does not belong to this showtime");
      }
      if (seat.getStatus() != SeatStatus.FREE) {
        throw new SeatConflictException("Seat not available: " + seat.getSeatCode());
      }
    }

    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(holdTtlMinutes);
    SeatHold hold =
        SeatHold.builder()
            .showtimeId(request.getShowtimeId())
            .expiresAt(expiresAt)
            .status(SeatHoldStatus.ACTIVE)
            .items(new ArrayList<>())
            .build();

    for (Seat seat : locked) {
      seat.setStatus(SeatStatus.HELD);
      SeatHoldItem item = SeatHoldItem.builder().seatHold(hold).seat(seat).build();
      hold.getItems().add(item);
    }

    SeatHold saved = seatHoldRepository.save(hold);
    return toHoldResponse(saved);
  }

  @Transactional(readOnly = true)
  public HoldResponse getHold(UUID holdId) {
    SeatHold hold =
        seatHoldRepository
            .findDetailById(holdId)
            .orElseThrow(() -> new ResourceNotFoundException("Hold not found: " + holdId));
    return toHoldResponse(hold);
  }

  @Transactional
  public void releaseHold(UUID holdId) {
    SeatHold hold =
        seatHoldRepository
            .findDetailById(holdId)
            .orElseThrow(() -> new ResourceNotFoundException("Hold not found: " + holdId));
    if (hold.getStatus() != SeatHoldStatus.ACTIVE) {
      throw new HoldConflictException("Hold cannot be released in state: " + hold.getStatus());
    }
    if (hold.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new HoldConflictException("Hold has expired");
    }
    for (SeatHoldItem item : hold.getItems()) {
      Seat seat = item.getSeat();
      if (seat.getStatus() == SeatStatus.HELD) {
        seat.setStatus(SeatStatus.FREE);
      }
    }
    hold.setStatus(SeatHoldStatus.RELEASED);
  }

  @Transactional
  public void confirmHold(UUID holdId) {
    SeatHold hold =
        seatHoldRepository
            .findDetailById(holdId)
            .orElseThrow(() -> new ResourceNotFoundException("Hold not found: " + holdId));
    if (hold.getStatus() != SeatHoldStatus.ACTIVE) {
      throw new HoldConflictException("Hold cannot be confirmed in state: " + hold.getStatus());
    }
    if (!hold.getExpiresAt().isAfter(LocalDateTime.now())) {
      throw new HoldConflictException("Hold has expired");
    }
    for (SeatHoldItem item : hold.getItems()) {
      Seat seat = item.getSeat();
      seat.setStatus(SeatStatus.BOOKED);
    }
    hold.setStatus(SeatHoldStatus.CONFIRMED);
  }

  @Transactional
  public void expireStaleHolds() {
    List<SeatHold> stale =
        seatHoldRepository.findActiveExpiredForUpdate(
            SeatHoldStatus.ACTIVE, LocalDateTime.now());
    for (SeatHold hold : stale) {
      for (SeatHoldItem item : hold.getItems()) {
        Seat seat = item.getSeat();
        if (seat.getStatus() == SeatStatus.HELD) {
          seat.setStatus(SeatStatus.FREE);
        }
      }
      hold.setStatus(SeatHoldStatus.EXPIRED);
    }
  }

  private List<Seat> loadSeatsForShowtime(Long showtimeId, List<Long> seatIds) {
    List<Seat> seats = seatRepository.findAllById(seatIds);
    if (seats.size() != seatIds.size()) {
      throw new SeatConflictException("One or more seats not found");
    }
    for (Seat seat : seats) {
      if (!seat.getShowtimeId().equals(showtimeId)) {
        throw new SeatConflictException("Seat does not belong to this showtime: " + seat.getSeatCode());
      }
    }
    return seats;
  }

  private HoldResponse toHoldResponse(SeatHold hold) {
    return HoldResponse.builder()
        .holdId(hold.getId())
        .showtimeId(hold.getShowtimeId())
        .status(hold.getStatus().name())
        .expiresAt(hold.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant())
        .seats(
            hold.getItems().stream()
                .map(
                    i -> {
                      Seat s = i.getSeat();
                      return HoldResponse.SeatInfo.builder()
                          .seatId(s.getId())
                          .seatCode(s.getSeatCode())
                          .rowLabel(s.getRowLabel())
                          .seatNumber(s.getSeatNumber())
                          .type(s.getType().name())
                          .price(s.getPrice())
                          .build();
                    })
                .collect(Collectors.toList()))
        .build();
  }
}
