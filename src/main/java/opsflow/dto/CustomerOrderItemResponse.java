package opsflow.dto;

import java.math.BigDecimal;

public record CustomerOrderItemResponse(
    Long id,
    Long productId,
    String productName,
    String sku,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {}