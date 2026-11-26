package dev.winestore.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record OrderResponse(long id, OrderStatus status, BigDecimal total, Instant createdAt,
                            List<OrderItemResponse> items) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                Objects.requireNonNull(order.getId()),
                order.getStatus(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(i -> new OrderItemResponse(i.getWineId(), i.getWineName(), i.getUnitPrice(), i.getQuantity()))
                        .toList());
    }
}
