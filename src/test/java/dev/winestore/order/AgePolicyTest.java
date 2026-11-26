package dev.winestore.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AgePolicyTest {

    private final LocalDate today = LocalDate.of(2026, 11, 19);

    @Test
    @DisplayName("exactly 18 years today - allowed")
    void exactly18() {
        assertThat(AgePolicy.isAdult(LocalDate.of(2008, 11, 19), today)).isTrue();
    }

    @Test
    @DisplayName("one day before 18 years - not allowed")
    void oneDayBefore18() {
        assertThat(AgePolicy.isAdult(LocalDate.of(2008, 11, 20), today)).isFalse();
    }

    @Test
    @DisplayName("adult - allowed")
    void adult() {
        assertThat(AgePolicy.isAdult(LocalDate.of(1985, 5, 1), today)).isTrue();
    }
}
