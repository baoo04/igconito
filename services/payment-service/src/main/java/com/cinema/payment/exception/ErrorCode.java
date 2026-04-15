package com.cinema.payment.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {

    PAYMENT_FAILED("PAYMENT_FAILED", "Payment declined", HttpStatus.PAYMENT_REQUIRED),
    BAD_REQUEST("BAD_REQUEST", "Invalid request", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    IDEMPOTENCY_REQUIRED("IDEMPOTENCY_REQUIRED", "Idempotency key is required", HttpStatus.BAD_REQUEST),

    ;
    String code;
    String message;
    HttpStatus statusCode;
}
