package com.cinema.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequest {

  @NotBlank private String fullName;

  @NotBlank @Email private String email;

  @NotBlank private String phone;
}
