package dev.winestore.cart;

import dev.winestore.AbstractIntegrationTest;
import dev.winestore.user.User;
import dev.winestore.wine.Wine;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getCart_withValidToken_returnsEmptyCart() throws Exception {
        User user = newUser();

        mockMvc.perform(get("/api/cart").with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    void addToCart_withValidRequest_returnsUpdatedCart() throws Exception {
        User user = newUser();
        Wine wine = newWine(10);

        mockMvc.perform(post("/api/cart/items")
                        .with(asUser(user))
                        .contentType("application/json")
                        .content("{\"wineId\": " + wine.getId() + ", \"quantity\": 2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    void addToCart_withDefaultQuantity_returnsUpdatedCart() throws Exception {
        User user = newUser();
        Wine wine = newWine(10);

        mockMvc.perform(post("/api/cart/items")
                        .with(asUser(user))
                        .contentType("application/json")
                        .content("{\"wineId\": " + wine.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void addToCart_withNonExistentWine_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .with(asUser(newUser()))
                        .contentType("application/json")
                        .content("{\"wineId\": 99999, \"quantity\": 1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeFromCart_withValidWineId_returnsUpdatedCart() throws Exception {
        User user = newUser();
        Wine wine = newWine(10);

        mockMvc.perform(post("/api/cart/items")
                        .with(asUser(user))
                        .contentType("application/json")
                        .content("{\"wineId\": " + wine.getId() + ", \"quantity\": 2}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/cart/items/" + wine.getId()).with(asUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void getCart_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addToCart_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType("application/json")
                        .content("{\"wineId\": 1, \"quantity\": 2}"))
                .andExpect(status().isUnauthorized());
    }
}
