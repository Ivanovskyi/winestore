package dev.winestore.wine;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

public record WineRequest(
        @NotBlank String name,
        @NotNull WineType type,
        @NotBlank String country,
        String region,
        @Min(1800) @Max(2100) Integer vintage,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotNull @Min(0) Integer stock,
        String description) {

    public Wine toEntity() {
        return new Wine(
                name.trim(), Objects.requireNonNull(type), country.trim(), region, vintage,
                Objects.requireNonNull(price), Objects.requireNonNull(stock), description);
    }
}
