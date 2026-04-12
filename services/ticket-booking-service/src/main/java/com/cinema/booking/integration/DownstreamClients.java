package com.cinema.booking.integration;

import com.cinema.booking.integration.dto.CustomerValidateResponseDto;
import com.cinema.booking.integration.dto.HoldResponseDto;
import com.cinema.booking.integration.dto.PaymentResponseDto;
import com.cinema.booking.integration.dto.ShowtimeResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class DownstreamClients {

  private final RestClient seatRestClient;
  private final RestClient customerRestClient;
  private final RestClient paymentRestClient;
  private final RestClient notificationRestClient;
  private final RestClient movieRestClient;
  private final JsonApiHelper jsonApiHelper;
  private final ObjectMapper objectMapper;

  public HoldResponseDto getHold(UUID holdId) throws IOException {
    try {
      String json =
          seatRestClient.get().uri("/holds/{id}", holdId).retrieve().body(String.class);
      return jsonApiHelper.unwrapData(json, HoldResponseDto.class);
    } catch (RestClientResponseException e) {
      if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
        return null;
      }
      throw e;
    }
  }

  public void confirmHold(UUID holdId) {
    seatRestClient
        .post()
        .uri("/holds/{id}/confirm", holdId)
        .retrieve()
        .toBodilessEntity();
  }

  public void releaseHold(UUID holdId) {
    seatRestClient
        .post()
        .uri("/holds/{id}/release", holdId)
        .retrieve()
        .toBodilessEntity();
  }

  public CustomerValidateResponseDto validateCustomer(String fullName, String email, String phone)
      throws IOException {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("fullName", fullName);
    body.put("email", email);
    body.put("phone", phone);
    try {
      String json =
          customerRestClient
              .post()
              .uri("/customers/validate")
              .contentType(MediaType.APPLICATION_JSON)
              .body(body.toString())
              .retrieve()
              .body(String.class);
      return jsonApiHelper.unwrapData(json, CustomerValidateResponseDto.class);
    } catch (RestClientResponseException e) {
      if (HttpStatus.BAD_REQUEST.equals(e.getStatusCode())) {
        return jsonApiHelper.unwrapData(e.getResponseBodyAsString(), CustomerValidateResponseDto.class);
      }
      throw e;
    }
  }

  public PaymentResponseDto createPayment(ObjectNode body) throws IOException {
    try {
      String json =
          paymentRestClient
              .post()
              .uri("/payments")
              .contentType(MediaType.APPLICATION_JSON)
              .body(body.toString())
              .retrieve()
              .body(String.class);
      return jsonApiHelper.unwrapData(json, PaymentResponseDto.class);
    } catch (RestClientResponseException e) {
      int code = e.getStatusCode().value();
      if (code == 402) {
        return jsonApiHelper.unwrapData(e.getResponseBodyAsString(), PaymentResponseDto.class);
      }
      throw e;
    }
  }

  public ShowtimeResponseDto getShowtime(long showtimeId) throws IOException {
    try {
      String json =
          movieRestClient.get().uri("/showtimes/{id}", showtimeId).retrieve().body(String.class);
      return jsonApiHelper.unwrapData(json, ShowtimeResponseDto.class);
    } catch (RestClientResponseException e) {
      if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
        return null;
      }
      throw e;
    }
  }

  public void sendNotification(ObjectNode body) {
    notificationRestClient
        .post()
        .uri("/notifications/booking-confirmations")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body.toString())
        .retrieve()
        .toBodilessEntity();
  }
}
