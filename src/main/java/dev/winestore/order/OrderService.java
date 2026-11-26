package dev.winestore.order;

import dev.winestore.cart.CartItem;
import dev.winestore.cart.CartItemRepository;
import dev.winestore.common.BadRequestException;
import dev.winestore.common.ConflictException;
import dev.winestore.common.ForbiddenOperationException;
import dev.winestore.common.NotFoundException;
import dev.winestore.payment.PaymentResult;
import dev.winestore.payment.PaymentService;
import dev.winestore.user.User;
import dev.winestore.user.UserRepository;
import dev.winestore.wine.Wine;
import dev.winestore.wine.WineCacheInvalidator;
import dev.winestore.wine.WineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final CartItemRepository cartItems;
    private final UserRepository users;
    private final WineRepository wines;
    private final PaymentService payments;
    private final WineCacheInvalidator wineCache;

    public OrderService(OrderRepository orders, CartItemRepository cartItems, UserRepository users,
                        WineRepository wines, PaymentService payments, WineCacheInvalidator wineCache) {
        this.orders = orders;
        this.cartItems = cartItems;
        this.users = users;
        this.wines = wines;
        this.payments = payments;
        this.wineCache = wineCache;
    }

    @Transactional
    public OrderResponse checkout(long userId) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!AgePolicy.isAdult(user.getBirthDate())) {
            throw new ForbiddenOperationException("Purchase available only from " + AgePolicy.MIN_AGE + " years old");
        }

        List<CartItem> cart = cartItems.findByUserId(userId);
        if (cart.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Order order = new Order(userId);
        List<Long> wineIds = new ArrayList<>();
        for (CartItem line : cart) {
            Wine wine = line.getWine();
            if (wine.getStock() < line.getQuantity()) {
                throw new ConflictException("Not enough '" + wine.getName() + "': " + wine.getStock() + " items left");
            }
            wine.setStock(wine.getStock() - line.getQuantity());
            long wineId = Objects.requireNonNull(wine.getId());
            order.addItem(wineId, wine.getName(), wine.getPrice(), line.getQuantity());
            wineIds.add(wineId);
        }
        order.recalculateTotal();

        orders.save(order);
        cartItems.deleteAllByUser(userId);
        wineCache.evictAfterCommit(wineIds);
        return OrderResponse.from(order);
    }

    @Transactional
    public PayResponse pay(long userId, long orderId) {
        Order order = findOwned(userId, orderId);
        if (order.getStatus() != OrderStatus.NEW) {
            throw new ConflictException("Order is " + order.getStatus() + ", payment not possible");
        }
        PaymentResult result = payments.charge(Objects.requireNonNull(order.getId()), order.getTotal());
        if (!result.success()) {
            throw new ConflictException("Payment failed: " + result.message());
        }

        order.setStatus(OrderStatus.PAID);
        return new PayResponse(OrderResponse.from(order), result.message());
    }

    @Transactional
    public OrderResponse cancel(long userId, long orderId) {
        Order order = findOwned(userId, orderId);
        if (order.getStatus() != OrderStatus.NEW) {
            throw new ConflictException("Can only cancel unpaid orders");
        }
        List<Long> wineIds = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            wines.findById(item.getWineId()).ifPresent(wine -> wine.setStock(wine.getStock() + item.getQuantity()));
            wineIds.add(item.getWineId());
        }
        order.setStatus(OrderStatus.CANCELLED);

        wineCache.evictAfterCommit(wineIds);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(long userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId).stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(long userId, long orderId) {
        return OrderResponse.from(findOwned(userId, orderId));
    }

    private Order findOwned(long userId, long orderId) {
        return orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
    }
}
