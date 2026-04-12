package com.cinema.movie.config;

import com.cinema.movie.entity.Auditorium;
import com.cinema.movie.entity.Cinema;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.entity.MovieStatus;
import com.cinema.movie.entity.Showtime;
import com.cinema.movie.entity.ShowtimeStatus;
import com.cinema.movie.repository.AuditoriumRepository;
import com.cinema.movie.repository.CinemaRepository;
import com.cinema.movie.repository.MovieRepository;
import com.cinema.movie.repository.ShowtimeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

  private final CinemaRepository cinemaRepository;
  private final AuditoriumRepository auditoriumRepository;
  private final MovieRepository movieRepository;
  private final ShowtimeRepository showtimeRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (cinemaRepository.count() > 0) {
      return;
    }
    log.info("Seeding movie_db with sample cinemas, movies, and showtimes");

    Cinema hanoi =
        cinemaRepository.save(
            Cinema.builder()
                .name("CGV Vincom Ba Trieu")
                .address("191 Ba Trieu, Hai Ba Trung")
                .city("Ha Noi")
                .phone("024-3974-0000")
                .build());
    Cinema hcm =
        cinemaRepository.save(
            Cinema.builder()
                .name("Lotte Cinema Cantavil")
                .address("136 Hai Ba Trung, Q1")
                .city("Ho Chi Minh")
                .phone("028-3820-0000")
                .build());
    Cinema danang =
        cinemaRepository.save(
            Cinema.builder()
                .name("Beta Cineplex Da Nang")
                .address("48 Tran Phu, Hai Chau")
                .city("Da Nang")
                .phone("0236-0000-000")
                .build());

    Auditorium a1 =
        auditoriumRepository.save(
            Auditorium.builder().cinema(hanoi).name("Hall 1").totalSeats(120).build());
    Auditorium a2 =
        auditoriumRepository.save(
            Auditorium.builder().cinema(hanoi).name("Hall 2").totalSeats(80).build());
    Auditorium b1 =
        auditoriumRepository.save(
            Auditorium.builder().cinema(hcm).name("Screen A").totalSeats(200).build());
    Auditorium b2 =
        auditoriumRepository.save(
            Auditorium.builder().cinema(hcm).name("Screen B").totalSeats(150).build());
    Auditorium c1 =
        auditoriumRepository.save(
            Auditorium.builder().cinema(danang).name("Hall VIP").totalSeats(60).build());

    Movie m1 =
        movieRepository.save(
            Movie.builder()
                .title("Dune: Part Three")
                .description("Sci-fi epic")
                .genre("Sci-Fi")
                .durationMinutes(165)
                .rating(8.4)
                .posterUrl("https://example.com/posters/dune3.jpg")
                .releaseDate(LocalDate.now().minusDays(7))
                .status(MovieStatus.ACTIVE)
                .build());
    Movie m2 =
        movieRepository.save(
            Movie.builder()
                .title("Quiet Place: Day One")
                .description("Horror prequel")
                .genre("Horror")
                .durationMinutes(99)
                .rating(7.1)
                .posterUrl("https://example.com/posters/quiet.jpg")
                .releaseDate(LocalDate.now().minusDays(14))
                .status(MovieStatus.ACTIVE)
                .build());
    Movie m3 =
        movieRepository.save(
            Movie.builder()
                .title("Inside Out 2")
                .description("Animation")
                .genre("Animation")
                .durationMinutes(96)
                .rating(7.8)
                .posterUrl("https://example.com/posters/insideout2.jpg")
                .releaseDate(LocalDate.now().minusDays(30))
                .status(MovieStatus.ACTIVE)
                .build());
    Movie m4 =
        movieRepository.save(
            Movie.builder()
                .title("Deadpool & Wolverine")
                .description("Action comedy")
                .genre("Action")
                .durationMinutes(128)
                .rating(8.0)
                .posterUrl("https://example.com/posters/dp.jpg")
                .releaseDate(LocalDate.now().minusDays(60))
                .status(MovieStatus.ACTIVE)
                .build());
    Movie m5 =
        movieRepository.save(
            Movie.builder()
                .title("Classic Re-run: Casablanca")
                .description("Classic drama")
                .genre("Drama")
                .durationMinutes(102)
                .rating(9.0)
                .posterUrl("https://example.com/posters/casa.jpg")
                .releaseDate(LocalDate.now().minusDays(365))
                .status(MovieStatus.INACTIVE)
                .build());

    LocalDateTime base = LocalDate.now().atTime(10, 0);
    showtimeRepository.save(
        show(m1, a1, base.plusHours(0), base.plusHours(3), "95000.00"));
    showtimeRepository.save(
        show(m1, a2, base.plusDays(1).plusHours(2), base.plusDays(1).plusHours(5), "85000.00"));
    showtimeRepository.save(
        show(m2, b1, base.plusHours(4), base.plusHours(6), "75000.00"));
    showtimeRepository.save(
        show(m3, b2, base.plusDays(1).plusHours(6), base.plusDays(1).plusHours(8), "90000.00"));
    showtimeRepository.save(
        show(m4, c1, base.plusDays(2).plusHours(1), base.plusDays(2).plusHours(3), "120000.00"));
    showtimeRepository.save(
        show(m1, b1, base.plusDays(2).plusHours(5), base.plusDays(2).plusHours(8), "110000.00"));
    showtimeRepository.save(
        show(m2, a1, base.plusDays(3).plusHours(0), base.plusDays(3).plusHours(2), "80000.00"));
    showtimeRepository.save(
        show(m3, a2, base.plusDays(3).plusHours(8), base.plusDays(3).plusHours(10), "88000.00"));
    showtimeRepository.save(
        show(m4, b2, base.plusDays(4).plusHours(3), base.plusDays(4).plusHours(5), "99000.00"));
    showtimeRepository.save(
        show(m2, c1, base.plusDays(5).plusHours(4), base.plusDays(5).plusHours(6), "105000.00"));
  }

  private Showtime show(Movie movie, Auditorium hall, LocalDateTime start, LocalDateTime end, String price) {
    return Showtime.builder()
        .movie(movie)
        .auditorium(hall)
        .startTime(start)
        .endTime(end)
        .basePrice(new BigDecimal(price))
        .status(ShowtimeStatus.SCHEDULED)
        .build();
  }
}
