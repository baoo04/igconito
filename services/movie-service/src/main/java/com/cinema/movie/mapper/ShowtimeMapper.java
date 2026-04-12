package com.cinema.movie.mapper;

import com.cinema.movie.dto.response.ShowtimeResponse;
import com.cinema.movie.entity.Showtime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ShowtimeMapper {

  @Mapping(target = "movieId", source = "movie.id")
  @Mapping(target = "movieTitle", source = "movie.title")
  @Mapping(target = "cinemaId", source = "auditorium.cinema.id")
  @Mapping(target = "cinemaName", source = "auditorium.cinema.name")
  @Mapping(target = "auditoriumId", source = "auditorium.id")
  @Mapping(target = "auditoriumName", source = "auditorium.name")
  ShowtimeResponse toResponse(Showtime showtime);
}
