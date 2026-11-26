package dev.winestore.cart;

import java.math.BigDecimal;

public record CartItemResponse(long wineId, String name, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
}
