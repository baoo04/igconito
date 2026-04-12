package com.cinema.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockEmailSender {

  private static final Logger log = LoggerFactory.getLogger(MockEmailSender.class);

  public void send(String to, String subject, String body) {
    log.info("MOCK EMAIL to={} | subject={} | bodyLength={}", to, subject, body.length());
  }
}
