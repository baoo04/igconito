package com.cinema.customer.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerResponse {

  private Long id;
  private String fullName;
  private String email;
  private String phone;
  private Instant createdAt;
}
