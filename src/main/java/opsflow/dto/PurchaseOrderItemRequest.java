
package opsflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record PurchaseOrderItemRequest(
    @NotNull Long productId,
    @NotNull @Positive Integer orderedQuantity,
    @NotNull @DecimalMin("0.0") BigDecimal unitPrice
) {}
