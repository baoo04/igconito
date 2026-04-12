package com.cinema.notification.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {

  private UUID notificationId;
  private String status;
  private String message;
}
