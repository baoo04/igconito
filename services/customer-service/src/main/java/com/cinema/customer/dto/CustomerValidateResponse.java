package com.cinema.customer.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerValidateResponse {

  private boolean valid;
  private Long customerId;
  private List<String> errors;
}
