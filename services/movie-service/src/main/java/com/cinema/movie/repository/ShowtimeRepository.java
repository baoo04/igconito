package com.cinema.movie.repository;

import com.cinema.movie.entity.Showtime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

  @Query(
      "SELECT s FROM Showtime s JOIN FETCH s.movie JOIN FETCH s.auditorium a JOIN FETCH a.cinema WHERE s.id = :id")
  Optional<Showtime> findDetailById(@Param("id") Long id);

  @EntityGraph(attributePaths = {"movie", "auditorium", "auditorium.cinema"})
  List<Showtime> findByMovieIdOrderByStartTimeAsc(Long movieId);

  @EntityGraph(attributePaths = {"movie", "auditorium", "auditorium.cinema"})
  List<Showtime> findByAuditoriumCinemaId(Long cinemaId);

  @EntityGraph(attributePaths = {"movie", "auditorium", "auditorium.cinema"})
  List<Showtime> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

  @EntityGraph(attributePaths = {"movie", "auditorium", "auditorium.cinema"})
  List<Showtime> findByAuditoriumCinemaIdAndStartTimeBetween(
      Long cinemaId, LocalDateTime start, LocalDateTime end);

  @EntityGraph(attributePaths = {"movie", "auditorium", "auditorium.cinema"})
  @Override
  List<Showtime> findAll();
}
