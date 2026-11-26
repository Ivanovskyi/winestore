package dev.winestore.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddToCartRequest(@NotNull Long wineId, @Min(1) Integer quantity) {

    public AddToCartRequest {
        if (quantity == null) {
            quantity = 1;
        }
    }
}
