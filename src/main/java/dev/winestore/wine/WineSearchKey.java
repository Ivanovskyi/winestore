package dev.winestore.wine;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;

public final class WineSearchKey {

    private WineSearchKey() {
    }

    public static String of(WineType type, String country, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return String.join("|",
                "type=" + (type == null ? "" : type.name()),
                "country=" + country(country),
                "min=" + price(minPrice),
                "max=" + price(maxPrice),
                "page=" + (pageable.isPaged() ? pageable.getPageNumber() + "," + pageable.getPageSize() : "unpaged"),
                "sort=" + sort(pageable.getSort()));
    }

    private static String country(String country) {
        if (country == null || country.isBlank()) {
            return "";
        }
        return encode(country.trim().toLowerCase(Locale.ROOT));
    }

    private static String price(BigDecimal price) {
        return price == null ? "" : price.stripTrailingZeros().toPlainString();
    }

    private static String sort(Sort sort) {
        return sort.stream()
                .map(o -> encode(o.getProperty()) + ":" + o.getDirection() + ":" + o.isIgnoreCase() + ":" + o.getNullHandling())
                .collect(Collectors.joining(","));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
