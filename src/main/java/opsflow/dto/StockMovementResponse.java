
package opsflow.dto;

import opsflow.entity.MovementType;
import java.time.LocalDateTime;

public record StockMovementResponse(
    Long id,
    Long inventoryId,
    MovementType movementType,
    Integer quantityChange,
    String reference,
    String notes,
    LocalDateTime createdAt
) {}
