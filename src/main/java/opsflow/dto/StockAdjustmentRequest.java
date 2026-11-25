
package opsflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
    @NotNull @Min(0) Integer targetQuantity,
    String reason
) {}
