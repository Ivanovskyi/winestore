package dev.winestore.order;

import dev.winestore.AbstractIntegrationTest;
import dev.winestore.user.User;
import dev.winestore.wine.Wine;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void checkout_withEmptyCart_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders").with(asUser(newUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_withItemsInCart_returnsOrderWithItems() throws Exception {
        User user = newUser();
        addToCart(user, newWine(10), 2);

        mockMvc.perform(post("/api/orders").with(asUser(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    void listOrders_withValidToken_returnsOrders() throws Exception {
        User user = newUser();
        long orderId = checkout(user);

        mockMvc.perform(get("/api/orders").with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(orderId));
    }

    @Test
    void getOrder_withValidToken_returnsOrder() throws Exception {
        User user = newUser();
        long orderId = checkout(user);

        mockMvc.perform(get("/api/orders/" + orderId).with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void getOrder_withNonExistentId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/99999").with(asUser(newUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void payOrder_withValidId_returnsPayResponse() throws Exception {
        User user = newUser();
        long orderId = checkout(user);

        mockMvc.perform(post("/api/orders/" + orderId + "/pay").with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.order.id").value(orderId));
    }

    @Test
    void cancelOrder_withNewOrder_returnsCancelledOrder() throws Exception {
        User user = newUser();
        long orderId = checkout(user);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel").with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void getOrders_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkout_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    private void addToCart(User user, Wine wine, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .with(asUser(user))
                        .contentType("application/json")
                        .content("{\"wineId\": " + wine.getId() + ", \"quantity\": " + quantity + "}"))
                .andExpect(status().isOk());
    }

    private long checkout(User user) throws Exception {
        addToCart(user, newWine(10), 1);
        return jsonId(mockMvc.perform(post("/api/orders").with(asUser(user)))
                .andExpect(status().isCreated())
                .andReturn());
    }
}
