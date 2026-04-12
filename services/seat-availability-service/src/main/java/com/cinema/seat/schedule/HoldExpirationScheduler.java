package com.cinema.seat.schedule;

import com.cinema.seat.service.HoldService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HoldExpirationScheduler {

  private static final Logger log = LoggerFactory.getLogger(HoldExpirationScheduler.class);

  private final HoldService holdService;

  @Scheduled(fixedRate = 60_000)
  public void expireStaleHolds() {
    try {
      holdService.expireStaleHolds();
    } catch (Exception e) {
      log.warn("Expire holds run failed: {}", e.getMessage());
    }
  }
}
