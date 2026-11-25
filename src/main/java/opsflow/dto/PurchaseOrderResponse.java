
package opsflow.dto;

import opsflow.entity.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
    Long id,
    Long supplierId,
    String supplierName,
    Long warehouseId,
    String warehouseName,
    PurchaseOrderStatus status,
    List<PurchaseOrderItemResponse> items,
    BigDecimal totalAmount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
