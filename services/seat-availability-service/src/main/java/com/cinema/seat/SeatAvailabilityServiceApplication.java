package com.cinema.seat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeatAvailabilityServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(SeatAvailabilityServiceApplication.class, args);
  }
}
