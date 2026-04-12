package com.cinema.movie.mapper;

import com.cinema.movie.dto.request.CreateCinemaRequest;
import com.cinema.movie.dto.response.CinemaResponse;
import com.cinema.movie.entity.Cinema;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CinemaMapper {

  CinemaResponse toResponse(Cinema cinema);

  Cinema toEntity(CreateCinemaRequest request);
}
