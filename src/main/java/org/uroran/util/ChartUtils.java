package org.uroran.util;

import org.uroran.models.Season;
import org.uroran.models.TemperatureData;

import java.time.LocalDate;
import java.time.Month;
import java.util.stream.Collectors;

/**
 * Класс, содержащий полезные методы для работы с графиком температур.
 */
public final class ChartUtils {
    private ChartUtils() {
    }

    /**
     * Получить доступные месяцы за определенный год из температурных данных.
     * @param data - данные.
     * @param year - год
     * @return список месяцев.
     */
    public static Month[] getAvailableMonthsForYear(TemperatureData data, int year) {
        return data.getData().keySet().stream()
                .filter(date -> date.getYear() == year)
                .map(LocalDate::getMonth)
                .toArray(Month[]::new);
    }

    /**
     * Получить доступные сезоны за определенный год из температурных данных.
     * @param data - данные.
     * @param year - год
     * @return список сезонов.
     */
    public static Season[] getAvailableSeasonsForYear(TemperatureData data, int year) {
        return data.getData().keySet().stream()
                .filter(e -> (!e.getMonth().equals(Month.NOVEMBER) && e.getDayOfMonth() != 27) && e.getYear() == year)
                .collect(Collectors.groupingBy(
                        date -> Season.getSeason(date.getMonth())
                ))
                .keySet().toArray(Season[]::new);
    }

    /**
     * Получить доступные года.
     * @param data - данные
     * @return - список годов
     */
    public static Integer[] getAvailableYears(TemperatureData data) {
        return data.getData().keySet().stream()
                .map(LocalDate::getYear)
                .distinct()
                .toArray(Integer[]::new);
    }
}
