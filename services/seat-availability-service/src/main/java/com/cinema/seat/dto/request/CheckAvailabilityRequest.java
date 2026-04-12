package com.cinema.seat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.Data;

@Data
public class CheckAvailabilityRequest {

  @NotEmpty private List<@Positive Long> seatIds;
}
