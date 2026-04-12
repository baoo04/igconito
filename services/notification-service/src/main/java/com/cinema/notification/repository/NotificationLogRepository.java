package com.cinema.notification.repository;

import com.cinema.notification.entity.NotificationLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {}
