package com.cinema.booking.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerValidateResponseDto {

  private boolean valid;
  private Long customerId;
  private List<String> errors;
}
