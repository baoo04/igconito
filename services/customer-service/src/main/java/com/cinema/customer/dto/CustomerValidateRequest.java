package com.cinema.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerValidateRequest {

  @NotBlank private String fullName;
  @NotBlank private String email;
  @NotBlank private String phone;
}
