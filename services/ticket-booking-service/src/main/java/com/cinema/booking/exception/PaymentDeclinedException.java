package com.cinema.booking.exception;

public class PaymentDeclinedException extends RuntimeException {

  public PaymentDeclinedException(String message) {
    super(message);
  }
}
