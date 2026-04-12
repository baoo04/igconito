package com.cinema.movie.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CinemaResponse {

  private Long id;
  private String name;
  private String address;
  private String city;
  private String phone;
}
