package dev.winestore.common;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static long userId(Jwt jwt) {
        return Long.parseLong(Objects.requireNonNull(jwt.getSubject(), "JWT subject is missing"));
    }
}
