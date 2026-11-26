package dev.winestore.user;

import dev.winestore.config.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AdminProperties props;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder, AdminProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByEmail(props.email())) {
            return;
        }
        users.save(new User(
                props.email(),
                Objects.requireNonNull(encoder.encode(props.password())),
                Role.ADMIN,
                LocalDate.of(1990, 1, 1)));
        log.info("Admin created {}", props.email());
    }
}
