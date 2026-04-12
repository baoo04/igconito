package com.cinema.seat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.Data;

@Data
public class CreateHoldRequest {

  @NotNull @Positive private Long showtimeId;

  @NotEmpty private List<@Positive Long> seatIds;
}
