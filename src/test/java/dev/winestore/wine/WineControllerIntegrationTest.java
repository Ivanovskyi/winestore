package dev.winestore.wine;

import dev.winestore.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WineControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String WINE_JSON = """
            {
                "name": "Test Wine",
                "type": "RED",
                "country": "Italy",
                "region": "Tuscany",
                "vintage": 2020,
                "price": 25.99,
                "stock": 100,
                "description": "A test wine"
            }
            """;

    private static final String UPDATED_WINE_JSON = """
            {
                "name": "Updated Wine",
                "type": "RED",
                "country": "France",
                "region": "Bordeaux",
                "vintage": 2019,
                "price": 35.99,
                "stock": 50,
                "description": "Updated description"
            }
            """;

    @Test
    void searchWines_withoutAuth_returnsPage() throws Exception {
        mockMvc.perform(get("/api/wines")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchWines_withFilters_returnsFilteredResults() throws Exception {
        mockMvc.perform(get("/api/wines")
                        .param("type", "RED")
                        .param("country", "France")
                        .param("minPrice", "10")
                        .param("maxPrice", "50")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getWineById_withoutAuth_returnsWine() throws Exception {
        Wine wine = newWine(5);

        mockMvc.perform(get("/api/wines/" + wine.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(wine.getId()))
                .andExpect(jsonPath("$.name").value("Test Wine"))
                .andExpect(jsonPath("$.price").exists());
    }

    @Test
    void getWineById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/wines/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWine_asAdmin_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/admin/wines")
                        .with(asAdmin())
                        .contentType("application/json")
                        .content(WINE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Wine"));
    }

    @Test
    void updateWine_asAdmin_returnsUpdated() throws Exception {
        Wine wine = newWine(5);

        mockMvc.perform(put("/api/admin/wines/" + wine.getId())
                        .with(asAdmin())
                        .contentType("application/json")
                        .content(UPDATED_WINE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Wine"));
    }

    @Test
    void deleteWine_asAdmin_returnsNoContent() throws Exception {
        Wine wine = newWine(5);

        mockMvc.perform(delete("/api/admin/wines/" + wine.getId())
                        .with(asAdmin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/wines/" + wine.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWine_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/wines")
                        .with(asUser(newUser()))
                        .contentType("application/json")
                        .content(WINE_JSON))
                .andExpect(status().isForbidden());
    }
}
