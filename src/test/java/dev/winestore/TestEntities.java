package dev.winestore;

import dev.winestore.cart.CartItem;
import dev.winestore.order.Order;
import dev.winestore.order.OrderStatus;
import dev.winestore.user.User;
import dev.winestore.wine.Wine;
import dev.winestore.wine.WineType;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TestEntities {

    private TestEntities() {
    }

    public static User user(long id, LocalDate birthDate) {
        User user = new User("test@example.com", "hash", birthDate);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Wine wine(long id, int stock) {
        Wine wine = new Wine("Test Wine", WineType.RED, "France", null, null, new BigDecimal("10.00"), stock, null);
        ReflectionTestUtils.setField(wine, "id", id);
        return wine;
    }

    public static CartItem cartItem(long id, long userId, Wine wine, int quantity) {
        CartItem item = new CartItem(userId, wine, quantity);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    public static Order order(long id, long userId, OrderStatus status, String total) {
        Order order = new Order(userId);
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "total", new BigDecimal(total));
        order.setStatus(status);
        return order;
    }
}
