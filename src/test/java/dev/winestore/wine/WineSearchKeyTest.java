package dev.winestore.wine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WineSearchKeyTest {

    private static final Pageable PAGE = PageRequest.of(0, 20, Sort.by("name"));

    private static String key(WineType type, String country, String min, String max, Pageable pageable) {
        return WineSearchKey.of(type, country,
                min == null ? null : new BigDecimal(min),
                max == null ? null : new BigDecimal(max),
                pageable);
    }

    @Test
    @DisplayName("equivalent filters share one key (case, whitespace, trailing zeros, blank country)")
    void equivalentFiltersShareKey() {
        String a = key(WineType.RED, "France", "10", "30.00", PAGE);
        String b = key(WineType.RED, "  france ", "10.0", "30", PAGE);

        assertThat(a).isEqualTo(b);
        assertThat(key(null, "   ", null, null, PAGE)).isEqualTo(key(null, null, null, null, PAGE));
    }

    @Test
    @DisplayName("different filters produce different keys")
    void differentFiltersDiffer() {
        String base = key(WineType.RED, "France", "10", "30", PAGE);

        assertThat(key(WineType.WHITE, "France", "10", "30", PAGE)).isNotEqualTo(base);
        assertThat(key(WineType.RED, "Italy", "10", "30", PAGE)).isNotEqualTo(base);
        assertThat(key(WineType.RED, "France", "11", "30", PAGE)).isNotEqualTo(base);
        assertThat(key(WineType.RED, "France", "10", "31", PAGE)).isNotEqualTo(base);
    }

    @Test
    @DisplayName("page number, page size and sort are part of the key")
    void paginationAndSortDiffer() {
        String base = key(null, null, null, null, PAGE);

        assertThat(key(null, null, null, null, PageRequest.of(1, 20, Sort.by("name")))).isNotEqualTo(base);
        assertThat(key(null, null, null, null, PageRequest.of(0, 50, Sort.by("name")))).isNotEqualTo(base);
        assertThat(key(null, null, null, null, PageRequest.of(0, 20, Sort.by("price")))).isNotEqualTo(base);
        assertThat(key(null, null, null, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "name"))))
                .isNotEqualTo(base);
    }

    @Test
    @DisplayName("min and max price are not interchangeable")
    void minAndMaxAreDistinct() {
        assertThat(key(null, null, "10", null, PAGE)).isNotEqualTo(key(null, null, null, "10", PAGE));
    }

    @Test
    @DisplayName("a country containing key delimiters cannot collide with a different query")
    void countryCannotInjectFields() {
        String injected = key(null, "x|min=1", null, null, PAGE);
        String genuine = key(null, "x", "1", null, PAGE);

        assertThat(injected).isNotEqualTo(genuine);
    }

    @Test
    @DisplayName("unpaged requests get their own key")
    void unpaged() {
        assertThat(key(null, null, null, null, Pageable.unpaged())).isNotEqualTo(key(null, null, null, null, PAGE));
    }
}
