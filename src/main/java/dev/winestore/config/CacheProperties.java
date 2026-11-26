package dev.winestore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("app.cache")
public record CacheProperties(
        @DefaultValue("10m") Duration wineTtl,
        @DefaultValue("2m") Duration searchTtl) {
}
