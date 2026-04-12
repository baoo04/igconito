package com.cinema.seat.exception;

public class HoldConflictException extends RuntimeException {

  public HoldConflictException(String message) {
    super(message);
  }
}
