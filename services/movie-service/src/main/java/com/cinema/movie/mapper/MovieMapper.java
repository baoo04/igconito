package com.cinema.movie.mapper;

import com.cinema.movie.dto.request.CreateMovieRequest;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MovieMapper {

  MovieResponse toResponse(Movie movie);

  Movie toEntity(CreateMovieRequest request);
}
