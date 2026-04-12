package com.cinema.movie.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCinemaRequest {

  @NotBlank private String name;
  private String address;
  private String city;
  private String phone;
}
