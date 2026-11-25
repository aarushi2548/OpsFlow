
package opsflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockOperationRequest(
    @NotNull @Positive Integer quantity,
    String reference,
    String notes
) {}
