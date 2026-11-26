package dev.winestore.wine;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public record WineResponse(
        long id,
        String name,
        WineType type,
        String country,
        String region,
        Integer vintage,
        BigDecimal price,
        int stock,
        String description) implements Serializable {

    public static WineResponse from(Wine wine) {
        return new WineResponse(
                Objects.requireNonNull(wine.getId()), wine.getName(), wine.getType(), wine.getCountry(),
                wine.getRegion(), wine.getVintage(), wine.getPrice(), wine.getStock(), wine.getDescription());
    }
}
