package com.cinema.movie.repository;

import com.cinema.movie.entity.Movie;
import com.cinema.movie.entity.MovieStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {

  List<Movie> findByStatus(MovieStatus status);

  List<Movie> findByGenreIgnoreCase(String genre);

  List<Movie> findByStatusAndGenreIgnoreCase(MovieStatus status, String genre);
}
