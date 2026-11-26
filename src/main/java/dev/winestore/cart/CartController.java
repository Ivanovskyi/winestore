package dev.winestore.cart;

import dev.winestore.common.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal Jwt jwt) {
        return cart.get(CurrentUser.userId(jwt));
    }

    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddToCartRequest req) {
        return cart.add(CurrentUser.userId(jwt), req);
    }

    @DeleteMapping("/items/{wineId}")
    public CartResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable long wineId) {
        return cart.remove(CurrentUser.userId(jwt), wineId);
    }
}
