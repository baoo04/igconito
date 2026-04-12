package com.cinema.booking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonApiHelper {

  private final ObjectMapper objectMapper;

  public <T> T unwrapData(String json, Class<T> type) throws IOException {
    JsonNode root = objectMapper.readTree(json);
    JsonNode data = root.get("data");
    if (data == null || data.isNull()) {
      return null;
    }
    return objectMapper.treeToValue(data, type);
  }
}
