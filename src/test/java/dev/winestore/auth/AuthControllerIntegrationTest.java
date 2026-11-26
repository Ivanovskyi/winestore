package dev.winestore.auth;

import dev.winestore.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void register_withValidData_returnsToken() throws Exception {
        String registerJson = """
            {
                "email": "%s@example.com",
                "password": "Password123!",
                "birthDate": "1990-01-01"
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").exists());
    }

    @Test
    void register_withExistingEmail_returnsConflict() throws Exception {
        String registerJson = """
            {
                "email": "admin@wine.local",
                "password": "Password123!",
                "birthDate": "1990-01-01"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withInvalidEmail_returnsBadRequest() throws Exception {
        String registerJson = """
            {
                "email": "invalid-email",
                "password": "Password123!",
                "birthDate": "1990-01-01"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        String loginJson = """
            {
                "email": "admin@wine.local",
                "password": "admin12345"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").exists());
    }

    @Test
    void login_withInvalidCredentials_returnsUnauthorized() throws Exception {
        String loginJson = """
            {
                "email": "admin@wine.local",
                "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withNonExistentUser_returnsUnauthorized() throws Exception {
        String loginJson = """
            {
                "email": "nonexistent@example.com",
                "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }
}
