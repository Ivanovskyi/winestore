package dev.winestore;

import com.jayway.jsonpath.JsonPath;
import dev.winestore.user.User;
import dev.winestore.user.UserRepository;
import dev.winestore.wine.Wine;
import dev.winestore.wine.WineRepository;
import dev.winestore.wine.WineType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@Import(TestcontainersConfig.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository users;
    @Autowired
    private WineRepository wines;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    protected User newUser() {
        return users.save(new User(UUID.randomUUID() + "@example.com", "hash", LocalDate.of(1990, 1, 1)));
    }

    protected Wine newWine(int stock) {
        return wines.save(new Wine("Test Wine", WineType.RED, "France", null, null, new BigDecimal("10.00"), stock, null));
    }

    protected static RequestPostProcessor asUser(User user) {
        return token(user.getId(), "USER");
    }

    protected RequestPostProcessor asAdmin() {
        return token(newUser().getId(), "ADMIN");
    }

    private static RequestPostProcessor token(long userId, String role) {
        return jwt()
                .jwt(j -> j.subject(Long.toString(userId)).claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    protected static long jsonId(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
