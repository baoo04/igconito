package com.cinema.booking.exception;

import com.cinema.booking.dto.ErrorResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ErrorResponse.builder()
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .status(404)
                .build());
  }

  @ExceptionHandler(SeatHoldInvalidException.class)
  public ResponseEntity<ErrorResponse> holdInvalid(SeatHoldInvalidException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ErrorResponse.builder()
                .error("SEAT_HOLD_INVALID")
                .message(ex.getMessage())
                .status(409)
                .build());
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> conflict(ConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ErrorResponse.builder()
                .error("CONFLICT")
                .message(ex.getMessage())
                .status(409)
                .build());
  }

  @ExceptionHandler(CustomerValidationFailedException.class)
  public ResponseEntity<ErrorResponse> customerInvalid(CustomerValidationFailedException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ErrorResponse.builder()
                .error("CUSTOMER_INVALID")
                .message(ex.getMessage())
                .status(400)
                .details(ex.getErrors())
                .build());
  }

  @ExceptionHandler(PaymentDeclinedException.class)
  public ResponseEntity<ErrorResponse> paymentFailed(PaymentDeclinedException ex) {
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
        .body(
            ErrorResponse.builder()
                .error("PAYMENT_DECLINED")
                .message(ex.getMessage())
                .status(402)
                .build());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
    String msg =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ErrorResponse.builder()
                .error("VALIDATION_ERROR")
                .message(msg)
                .status(400)
                .build());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message(ex.getMessage())
                .status(400)
                .build());
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<ErrorResponse> io(IOException ex) {
    log.warn("IO error: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(
            ErrorResponse.builder()
                .error("IO_ERROR")
                .message("Could not read downstream response")
                .status(502)
                .build());
  }

  @ExceptionHandler(UncheckedIOException.class)
  public ResponseEntity<ErrorResponse> uncheckedIo(UncheckedIOException ex) {
    log.warn("Unchecked IO: {}", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(
            ErrorResponse.builder()
                .error("IO_ERROR")
                .message("Could not read downstream response")
                .status(502)
                .build());
  }

  @ExceptionHandler(RestClientException.class)
  public ResponseEntity<ErrorResponse> upstream(RestClientException ex) {
    log.warn("Upstream call failed: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(
            ErrorResponse.builder()
                .error("UPSTREAM_ERROR")
                .message("A downstream service call failed")
                .status(502)
                .build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> generic(Exception ex) {
    log.error("Unexpected error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .status(500)
                .build());
  }
}
