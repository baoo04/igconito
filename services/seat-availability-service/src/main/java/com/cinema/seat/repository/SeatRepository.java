package com.cinema.seat.repository;

import com.cinema.seat.entity.Seat;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

  long countByShowtimeId(Long showtimeId);

  List<Seat> findByShowtimeIdOrderByRowLabelAscSeatNumberAsc(Long showtimeId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM Seat s WHERE s.id IN :ids ORDER BY s.id")
  List<Seat> lockByIds(@Param("ids") Collection<Long> ids);
}
