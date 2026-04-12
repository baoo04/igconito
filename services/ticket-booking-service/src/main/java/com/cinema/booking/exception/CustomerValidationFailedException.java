package com.cinema.booking.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class CustomerValidationFailedException extends RuntimeException {

  private final List<String> errors;

  public CustomerValidationFailedException(List<String> errors) {
    super("Customer validation failed");
    this.errors = errors;
  }
}
