package org.uroran.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер файлов с температурой в скважинах
 */
public final class PointParser {
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{1,2}-\\d{1,2})");

    private PointParser() {
    }

    /**
     * Метод для парсинга файла с температурами.
     * @param path - путь к файлу
     * @return - словарь, где ключ - это дата, значение - это словарь глубина-температура.
     */
    public static Map<LocalDate, Map<Double, Double>> parsePointFile(String path) throws IOException {
        Path pathToFile = Path.of(path);
        String text = Files.readString(pathToFile).trim();
        String[] monthsProfiles = text.split("\n\n");

        monthsProfiles = Arrays.stream(monthsProfiles)
                .toArray(String[]::new);

        Map<LocalDate, Map<Double, Double>> map = new LinkedHashMap<>();

        Arrays.stream(monthsProfiles).toList().forEach(profile -> fillMap(profile, map));

        return map;
    }

    /**
     * Приватный метод для заполнения словаря.
     * @param profile - блок текста с содержимым температур на разной глубине за 1 месяц.
     * @param map - словарь для заполнения.
     */
    private static void fillMap(String profile, Map<LocalDate, Map<Double, Double>> map) {
        Matcher matcher = DATE_PATTERN.matcher(profile);
        if (matcher.find()) {
            LocalDate keyDate = LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-M-d"));

            Map<Double, Double> depthToTemperature = new LinkedHashMap<>();
            String[] depths = profile.split("\n")[2].trim().split(" ");
            String[] temps = profile.split("\n")[3].trim().split(" ");

            for (int i = 0; i < depths.length; i++) {
                double depth = Double.parseDouble(depths[i]);
                double temp = Double.parseDouble(temps[i]);
                depthToTemperature.put(depth, temp);
            }

            map.put(keyDate, depthToTemperature);
        }
    }
}
