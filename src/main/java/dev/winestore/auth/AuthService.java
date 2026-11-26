package dev.winestore.auth;

import dev.winestore.common.ConflictException;
import dev.winestore.common.UnauthorizedException;
import dev.winestore.user.User;
import dev.winestore.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        User user = users.save(new User(
                email,
                Objects.requireNonNull(encoder.encode(req.password())),
                req.birthDate()));
        return jwt.issue(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        User user = users.findByEmail(normalize(req.email())).orElse(null);
        if (user == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return jwt.issue(user);
    }

    private static String normalize(String email) {

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
