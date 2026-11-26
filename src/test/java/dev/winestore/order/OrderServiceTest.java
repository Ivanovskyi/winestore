package dev.winestore.order;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static dev.winestore.TestEntities.cartItem;
import static dev.winestore.TestEntities.order;
import static dev.winestore.TestEntities.user;
import static dev.winestore.TestEntities.wine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final LocalDate ADULT_BIRTH = LocalDate.of(1990, 1, 1);

    @Mock
    private OrderRepository orders;
    @Mock
    private CartItemRepository cartItems;
    @Mock
    private UserRepository users;
    @Mock
    private WineRepository wines;
    @Mock
    private PaymentService payments;
    @Mock
    private WineCacheInvalidator wineCache;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, cartItems, users, wines, payments, wineCache);
    }

    private void stubSave() {
        when(orders.save(any(Order.class))).thenAnswer(inv -> {
            Order saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
    }

    // ---------- checkout ----------

    @Test
    @DisplayName("checkout - success - stock deducted, cart cleared, cache evicted")
    void checkoutSuccess() {
        Wine wine = wine(1L, 10);
        when(users.findById(1L)).thenReturn(Optional.of(user(1L, ADULT_BIRTH)));
        when(cartItems.findByUserId(1L)).thenReturn(List.of(cartItem(1L, 1L, wine, 2)));
        stubSave();

        OrderResponse result = service.checkout(1L);

        assertThat(wine.getStock()).isEqualTo(8);
        assertThat(result.total()).isEqualByComparingTo("20.00");
        assertThat(result.items()).hasSize(1);
        verify(orders).save(any(Order.class));
        verify(cartItems).deleteAllByUser(1L);
        verify(wineCache).evictAfterCommit(List.of(1L));
    }

    @Test
    @DisplayName("checkout - user not found - throws NotFoundException")
    void checkoutUserNotFound() {
        when(users.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkout(1L)).isInstanceOf(NotFoundException.class);

        verifyNoInteractions(cartItems);
    }

    @Test
    @DisplayName("checkout - underage - throws ForbiddenOperationException")
    void checkoutUnderage() {
        User child = user(1L, LocalDate.of(2015, 1, 1));
        when(users.findById(1L)).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> service.checkout(1L)).isInstanceOf(ForbiddenOperationException.class);

        verifyNoInteractions(cartItems);
    }

    @Test
    @DisplayName("checkout - empty cart - throws BadRequestException")
    void checkoutEmptyCart() {
        when(users.findById(1L)).thenReturn(Optional.of(user(1L, ADULT_BIRTH)));
        when(cartItems.findByUserId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.checkout(1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("checkout - insufficient stock - throws ConflictException, nothing saved or evicted")
    void checkoutInsufficientStock() {
        Wine wine = wine(1L, 1);
        when(users.findById(1L)).thenReturn(Optional.of(user(1L, ADULT_BIRTH)));
        when(cartItems.findByUserId(1L)).thenReturn(List.of(cartItem(1L, 1L, wine, 5)));

        assertThatThrownBy(() -> service.checkout(1L)).isInstanceOf(ConflictException.class);

        verify(orders, never()).save(any());
        verifyNoInteractions(wineCache);
    }

    @Test
    @DisplayName("checkout - exactly 18 years old - allowed")
    void checkoutExactly18() {
        User eighteen = user(1L, LocalDate.now().minusYears(18));
        when(users.findById(1L)).thenReturn(Optional.of(eighteen));
        when(cartItems.findByUserId(1L)).thenReturn(List.of(cartItem(1L, 1L, wine(1L, 10), 2)));
        stubSave();

        assertThat(service.checkout(1L).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("pay - success")
    void paySuccess() {
        Order order = order(1L, 1L, OrderStatus.NEW, "20.00");
        when(orders.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));
        when(payments.charge(1L, new BigDecimal("20.00"))).thenReturn(new PaymentResult(true, "Payment successful"));

        PayResponse result = service.pay(1L, 1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.message()).isEqualTo("Payment successful");
        verifyNoInteractions(wineCache);
    }

    @Test
    @DisplayName("pay - order not found - throws NotFoundException")
    void payOrderNotFound() {
        when(orders.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pay(1L, 1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("pay - already paid - throws ConflictException")
    void payAlreadyPaid() {
        when(orders.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order(1L, 1L, OrderStatus.PAID, "20.00")));

        assertThatThrownBy(() -> service.pay(1L, 1L)).isInstanceOf(ConflictException.class);

        verifyNoInteractions(payments);
    }

    @Test
    @DisplayName("pay - payment failed - throws ConflictException")
    void payFailed() {
        Order order = order(1L, 1L, OrderStatus.NEW, "20.00");
        when(orders.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));
        when(payments.charge(1L, new BigDecimal("20.00"))).thenReturn(new PaymentResult(false, "Card declined"));

        assertThatThrownBy(() -> service.pay(1L, 1L)).isInstanceOf(ConflictException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    @DisplayName("cancel - success - stock returned and cache evicted")
    void cancelSuccess() {
        Order order = order(1L, 1L, OrderStatus.NEW, "20.00");
        order.addItem(1L, "Test Wine", new BigDecimal("10.00"), 2);
        Wine wine = wine(1L, 8);

        when(orders.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));
        when(wines.findById(1L)).thenReturn(Optional.of(wine));

        service.cancel(1L, 1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(wine.getStock()).isEqualTo(10);
        verify(wineCache).evictAfterCommit(List.of(1L));
    }

    @Test
    @DisplayName("cancel - order not found - throws NotFoundException")
    void cancelOrderNotFound() {
        when(orders.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(1L, 1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("cancel - already paid - throws ConflictException")
    void cancelAlreadyPaid() {
        when(orders.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order(1L, 1L, OrderStatus.PAID, "20.00")));

        assertThatThrownBy(() -> service.cancel(1L, 1L)).isInstanceOf(ConflictException.class);

        verify(wines, never()).findById(anyLong());
        verifyNoInteractions(wineCache);
    }

    @Test
    @DisplayName("list - returns user orders")
    void listReturnsOrders() {
        when(orders.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(order(1L, 1L, OrderStatus.NEW, "20.00")));

        List<OrderResponse> result = service.list(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("get - returns user order")
    void getReturnsOrder() {
        when(orders.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order(1L, 1L, OrderStatus.NEW, "20.00")));

        assertThat(service.get(1L, 1L).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("get - order not found - throws NotFoundException")
    void getOrderNotFound() {
        when(orders.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L, 1L)).isInstanceOf(NotFoundException.class);
    }
}
