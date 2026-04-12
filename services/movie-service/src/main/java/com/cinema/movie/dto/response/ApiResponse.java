package com.cinema.movie.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  private T data;
  private String message;
  private int status;

  public static <T> ApiResponse<T> ok(T data) {
    return ApiResponse.<T>builder().data(data).message("Success").status(200).build();
  }

  public static <T> ApiResponse<T> created(T data) {
    return ApiResponse.<T>builder().data(data).message("Created").status(201).build();
  }
}
