package dev.winestore.cart;

import dev.winestore.common.ConflictException;
import dev.winestore.common.NotFoundException;
import dev.winestore.wine.Wine;
import dev.winestore.wine.WineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static dev.winestore.TestEntities.cartItem;
import static dev.winestore.TestEntities.wine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository items;
    @Mock
    private WineRepository wines;

    private CartService service;

    @BeforeEach
    void setUp() {
        service = new CartService(items, wines);
    }

    @Test
    @DisplayName("get - returns cart for user")
    void getReturnsCart() {
        Wine wine = wine(1L, 5);
        when(items.findByUserId(1L)).thenReturn(List.of(cartItem(1L, 1L, wine, 2)));

        CartResponse result = service.get(1L);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Test Wine");
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
        assertThat(result.total()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("get - empty cart")
    void getEmptyCart() {
        when(items.findByUserId(1L)).thenReturn(List.of());

        CartResponse result = service.get(1L);

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("add - new item to cart")
    void addNewItem() {
        Wine wine = wine(1L, 10);
        CartItem saved = cartItem(1L, 1L, wine, 2);

        when(wines.findById(1L)).thenReturn(Optional.of(wine));
        when(items.findOne(1L, 1L)).thenReturn(Optional.empty());
        when(items.findByUserId(1L)).thenReturn(List.of(saved));

        CartResponse result = service.add(1L, new AddToCartRequest(1L, 2));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
        verify(items).save(any(CartItem.class));
    }

    @Test
    @DisplayName("add - existing item - quantity increased")
    void addExistingItem() {
        Wine wine = wine(1L, 10);
        CartItem existing = cartItem(1L, 1L, wine, 3);

        when(wines.findById(1L)).thenReturn(Optional.of(wine));
        when(items.findOne(1L, 1L)).thenReturn(Optional.of(existing));
        when(items.findByUserId(1L)).thenReturn(List.of(existing));

        CartResponse result = service.add(1L, new AddToCartRequest(1L, 2));

        assertThat(existing.getQuantity()).isEqualTo(5);
        assertThat(result.items().get(0).quantity()).isEqualTo(5);
        verify(items).save(existing);
    }

    @Test
    @DisplayName("add - quantity defaults to 1 when omitted")
    void addDefaultQuantity() {
        assertThat(new AddToCartRequest(1L, null).quantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("add - wine not found - throws NotFoundException")
    void addWineNotFound() {
        when(wines.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(1L, new AddToCartRequest(1L, 2)))
                .isInstanceOf(NotFoundException.class);

        verify(items, never()).save(any());
    }

    @Test
    @DisplayName("add - exceeds stock - throws ConflictException")
    void addExceedsStock() {
        Wine wine = wine(1L, 5);
        when(wines.findById(1L)).thenReturn(Optional.of(wine));
        when(items.findOne(1L, 1L)).thenReturn(Optional.of(cartItem(1L, 1L, wine, 3)));

        assertThatThrownBy(() -> service.add(1L, new AddToCartRequest(1L, 8)))
                .isInstanceOf(ConflictException.class);

        verify(items, never()).save(any());
    }

    @Test
    @DisplayName("add - exactly at stock limit - allowed")
    void addAtStockLimit() {
        Wine wine = wine(1L, 5);
        CartItem saved = cartItem(1L, 1L, wine, 5);

        when(wines.findById(1L)).thenReturn(Optional.of(wine));
        when(items.findOne(1L, 1L)).thenReturn(Optional.empty());
        when(items.findByUserId(1L)).thenReturn(List.of(saved));

        CartResponse result = service.add(1L, new AddToCartRequest(1L, 5));

        assertThat(result.items().get(0).quantity()).isEqualTo(5);
        verify(items).save(any(CartItem.class));
    }

    @Test
    @DisplayName("remove - removes item from cart")
    void removeItem() {
        when(items.findByUserId(1L)).thenReturn(List.of());

        CartResponse result = service.remove(1L, 1L);

        assertThat(result.items()).isEmpty();
        verify(items).deleteOne(1L, 1L);
    }
}
