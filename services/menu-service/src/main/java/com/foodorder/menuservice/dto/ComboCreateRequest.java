package com.foodorder.menuservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ComboCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull @Positive BigDecimal bundlePrice,
        boolean available,
        @NotEmpty @Valid List<ComboItemLineRequest> items) {}
