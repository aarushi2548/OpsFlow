package opsflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CustomerOrderItemRequest(
    @NotNull Long productId,
    @NotNull @Positive Integer quantity
) {}