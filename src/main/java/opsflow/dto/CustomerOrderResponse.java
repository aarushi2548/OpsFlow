package opsflow.dto;

import opsflow.entity.CustomerOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerOrderResponse(
    Long id,
    Long customerId,
    String customerName,
    Long warehouseId,
    String warehouseName,
    CustomerOrderStatus status,
    List<CustomerOrderItemResponse> items,
    BigDecimal totalAmount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}