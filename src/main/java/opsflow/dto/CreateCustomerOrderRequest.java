package opsflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateCustomerOrderRequest(
    @NotNull Long customerId,
    @NotNull Long warehouseId,
    @NotEmpty List<@Valid CustomerOrderItemRequest> items
) {}