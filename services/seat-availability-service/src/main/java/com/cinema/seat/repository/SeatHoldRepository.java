package com.cinema.seat.repository;

import com.cinema.seat.entity.SeatHold;
import com.cinema.seat.entity.SeatHoldStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatHoldRepository extends JpaRepository<SeatHold, UUID> {

  @Query(
      "SELECT DISTINCT h FROM SeatHold h LEFT JOIN FETCH h.items i LEFT JOIN FETCH i.seat WHERE h.id = :id")
  Optional<SeatHold> findDetailById(@Param("id") UUID id);

  @Query(
      "SELECT DISTINCT h FROM SeatHold h JOIN FETCH h.items i JOIN FETCH i.seat WHERE h.status = :status AND h.expiresAt < :before")
  List<SeatHold> findActiveExpiredForUpdate(
      @Param("status") SeatHoldStatus status, @Param("before") LocalDateTime before);
}
