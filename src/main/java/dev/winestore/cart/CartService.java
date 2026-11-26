package dev.winestore.cart;

import dev.winestore.common.ConflictException;
import dev.winestore.common.NotFoundException;
import dev.winestore.wine.Wine;
import dev.winestore.wine.WineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Works with the database directly (never with the Redis catalog cache): the stock check must be authoritative.
 */
@Service
public class CartService {

    private final CartItemRepository items;
    private final WineRepository wines;

    public CartService(CartItemRepository items, WineRepository wines) {
        this.items = items;
        this.wines = wines;
    }

    @Transactional(readOnly = true)
    public CartResponse get(long userId) {
        return CartResponse.from(items.findByUserId(userId));
    }

    @Transactional
    public CartResponse add(long userId, AddToCartRequest req) {
        long wineId = Objects.requireNonNull(req.wineId());
        Wine wine = wines.findById(wineId)
                .orElseThrow(() -> new NotFoundException("Wine " + wineId + " not found"));
        CartItem item = items.findOne(userId, wineId)
                .orElseGet(() -> new CartItem(userId, wine, 0));

        int newQuantity = item.getQuantity() + req.quantity();
        if (newQuantity > wine.getStock()) {
            throw new ConflictException("Only " + wine.getStock() + " items in stock");
        }

        item.setQuantity(newQuantity);
        items.save(item);
        return CartResponse.from(items.findByUserId(userId));
    }

    @Transactional
    public CartResponse remove(long userId, long wineId) {
        items.deleteOne(userId, wineId);
        return CartResponse.from(items.findByUserId(userId));
    }
}
