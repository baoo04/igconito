package com.cinema.booking.repository;

import com.cinema.booking.entity.Booking;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

  Optional<Booking> findByBookingCode(String bookingCode);

  @EntityGraph(attributePaths = {"tickets"})
  @Query("SELECT b FROM Booking b WHERE b.id = :id")
  Optional<Booking> findWithTicketsById(@Param("id") UUID id);
}
