
package opsflow.dto;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
    Long id,
    Long productId,
    String productName,
    String sku,
    Integer orderedQuantity,
    Integer receivedQuantity,
    Integer pendingQuantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {}
