
package opsflow.dto;

import java.time.LocalDateTime;

public record InventoryResponse(
    Long id,
    Long warehouseId,
    String warehouseName,
    Long productId,
    String productName,
    String sku,
    Integer quantityOnHand,
    Integer reorderLevel,
    boolean lowStock,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
