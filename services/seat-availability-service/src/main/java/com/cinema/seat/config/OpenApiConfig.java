package com.cinema.seat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI seatServiceOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Seat Availability Service API")
                .description("Seat maps, holds, and availability checks")
                .version("v1"));
  }
}
