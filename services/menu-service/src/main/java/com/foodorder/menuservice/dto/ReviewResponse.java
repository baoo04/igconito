package com.foodorder.menuservice.dto;

import java.time.Instant;

public record ReviewResponse(Long id, int rating, String comment, String authorName, Instant createdAt) {}
