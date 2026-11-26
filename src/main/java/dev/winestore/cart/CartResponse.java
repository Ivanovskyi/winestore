package dev.winestore.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record CartResponse(List<CartItemResponse> items, BigDecimal total) {

    public static CartResponse from(List<CartItem> cartItems) {
        List<CartItemResponse> lines = cartItems.stream()
                .map(item -> new CartItemResponse(
                        Objects.requireNonNull(item.getWine().getId()),
                        item.getWine().getName(),
                        item.getWine().getPrice(),
                        item.getQuantity(),
                        item.getWine().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .toList();
        BigDecimal total = lines.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(lines, total);
    }
}
