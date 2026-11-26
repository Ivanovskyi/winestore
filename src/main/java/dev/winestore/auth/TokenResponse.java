package dev.winestore.auth;

public record TokenResponse(String accessToken, long expiresInSeconds, String tokenType) {

    public TokenResponse(String accessToken, long expiresInSeconds) {

        this(accessToken, expiresInSeconds, "Bearer");
    }
}
