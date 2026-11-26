package dev.winestore.auth;

import dev.winestore.common.ConflictException;
import dev.winestore.common.UnauthorizedException;
import dev.winestore.user.User;
import dev.winestore.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static dev.winestore.TestEntities.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final LocalDate BIRTH = LocalDate.of(1990, 1, 1);

    @Mock
    private UserRepository users;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private JwtService jwt;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, encoder, jwt);
    }

    @Test
    @DisplayName("register - success")
    void registerSuccess() {
        RegisterRequest req = new RegisterRequest("test@example.com", "password123", BIRTH);
        User user = user(1L, BIRTH);
        TokenResponse token = new TokenResponse("token", 3600);

        when(users.existsByEmail("test@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hashed");
        when(users.save(any(User.class))).thenReturn(user);
        when(jwt.issue(user)).thenReturn(token);

        TokenResponse result = service.register(req);

        assertThat(result).isEqualTo(token);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getBirthDate()).isEqualTo(BIRTH);
        verify(jwt).issue(user);
    }

    @Test
    @DisplayName("register - duplicate email - throws ConflictException")
    void registerDuplicateEmail() {
        RegisterRequest req = new RegisterRequest("test@example.com", "password123", BIRTH);
        when(users.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(req)).isInstanceOf(ConflictException.class);

        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("register - email trimmed and lowercased")
    void registerNormalizesEmail() {
        RegisterRequest req = new RegisterRequest("  Test@Example.COM  ", "password123", BIRTH);
        User user = user(1L, BIRTH);

        when(users.existsByEmail("test@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hashed");
        when(users.save(any(User.class))).thenReturn(user);
        when(jwt.issue(user)).thenReturn(new TokenResponse("token", 3600));

        service.register(req);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("login - success")
    void loginSuccess() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        User user = user(1L, BIRTH);
        TokenResponse token = new TokenResponse("token", 3600);

        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password123", "hash")).thenReturn(true);
        when(jwt.issue(user)).thenReturn(token);

        assertThat(service.login(req)).isEqualTo(token);
    }

    @Test
    @DisplayName("login - user not found - throws UnauthorizedException")
    void loginUserNotFound() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        when(users.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(req)).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(encoder);
    }

    @Test
    @DisplayName("login - wrong password - throws UnauthorizedException")
    void loginWrongPassword() {
        LoginRequest req = new LoginRequest("test@example.com", "wrongpassword");
        User user = user(1L, BIRTH);

        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrongpassword", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(req)).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(jwt);
    }

    @Test
    @DisplayName("login - email trimmed and lowercased")
    void loginNormalizesEmail() {
        LoginRequest req = new LoginRequest("  Test@Example.COM  ", "password123");
        User user = user(1L, BIRTH);

        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwt.issue(user)).thenReturn(new TokenResponse("token", 3600));

        service.login(req);

        verify(users).findByEmail("test@example.com");
    }
}
