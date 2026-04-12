package com.cinema.movie.repository;

import com.cinema.movie.entity.Auditorium;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriumRepository extends JpaRepository<Auditorium, Long> {

  List<Auditorium> findByCinemaId(Long cinemaId);
}
