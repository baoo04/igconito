package com.foodorder.menuservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ComboItemLineRequest(@NotNull Long foodItemId, @NotNull @Min(1) Integer quantity) {}
