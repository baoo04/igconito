package com.cinema.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfiguration {

  @Bean
  RestClient seatRestClient(@Value("${clients.seat.base-url}") String base) {
    return RestClient.builder().baseUrl(base).build();
  }

  @Bean
  RestClient customerRestClient(@Value("${clients.customer.base-url}") String base) {
    return RestClient.builder().baseUrl(base).build();
  }

  @Bean
  RestClient paymentRestClient(@Value("${clients.payment.base-url}") String base) {
    return RestClient.builder().baseUrl(base).build();
  }

  @Bean
  RestClient notificationRestClient(@Value("${clients.notification.base-url}") String base) {
    return RestClient.builder().baseUrl(base).build();
  }

  @Bean
  RestClient movieRestClient(@Value("${clients.movie.base-url}") String base) {
    return RestClient.builder().baseUrl(base).build();
  }
}
