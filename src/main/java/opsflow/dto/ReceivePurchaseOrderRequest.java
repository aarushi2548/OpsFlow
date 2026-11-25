
package opsflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReceivePurchaseOrderRequest(
    @NotNull Long itemId,
    @NotNull @Positive Integer quantityReceived,
    String reference,
    String notes
) {}
