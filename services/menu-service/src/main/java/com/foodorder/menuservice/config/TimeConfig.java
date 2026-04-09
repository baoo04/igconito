package com.foodorder.menuservice.config;

import java.time.Clock;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TimeConfig {

    private final MenuPricingProperties menuPricingProperties;

    @Bean
    public Clock menuClock() {
        return Clock.system(ZoneId.of(menuPricingProperties.getZoneId()));
    }
}
