package org.uroran.models;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Month;

/**
 * Enum для сезонов года
 */
@Getter
public enum Season {
    WINTER("Зима"),
    SPRING("Весна"),
    SUMMER("Лето"),
    AUTUMN("Осень");

    private final String russianTranslation;

    Season(String russianTranslation) {
        this.russianTranslation = russianTranslation;
    }

    public static Season getSeason(@NotNull Month month) {
        return switch (month) {
            case DECEMBER, JANUARY, FEBRUARY -> WINTER;
            case MARCH, APRIL, MAY -> SPRING;
            case JUNE, JULY, AUGUST -> SUMMER;
            case SEPTEMBER, OCTOBER, NOVEMBER -> AUTUMN;
        };
    }
}
