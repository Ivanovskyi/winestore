package dev.winestore.order;

import java.time.LocalDate;

public final class AgePolicy {

    public static final int MIN_AGE = 18;

    private AgePolicy() {
    }

    public static boolean isAdult(LocalDate birthDate) {
        return isAdult(birthDate, LocalDate.now());
    }

    public static boolean isAdult(LocalDate birthDate, LocalDate today) {
        return !birthDate.plusYears(MIN_AGE).isAfter(today);
    }
}
