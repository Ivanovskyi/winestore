package dev.winestore.order;

import java.math.BigDecimal;

public record OrderItemResponse(long wineId, String name, BigDecimal unitPrice, int quantity) {
}
