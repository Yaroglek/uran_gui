package org.uroran.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Класс, содержащий температурные профили в определенной скважине
 */
@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TemperatureData {
    private int pointNumber;
    private Map<LocalDate, Map<Double, Double>> data; // Date - Depth - Temperature
}
