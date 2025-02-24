package org.uroran.util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.uroran.models.Season;
import org.uroran.models.TemperatureData;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

/**
 * Класс, содержащий методы для отрисовки панели с графиком
 */
public final class ChartDrawer {
    private ChartDrawer() {
    }

    public static ChartPanel drawMonthChart(LocalDate date, TemperatureData data) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        var monthData = data.getData().get(date);
        addTemperatureDataToDataset(dataset, date, monthData);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Температура за " + date.getMonth().toString() + " " + date.getYear(),
                "Дата",
                "Температура",
                dataset
        );

        chart.getXYPlot().getDomainAxis().setRange(0.0, 12.0);
        return new ChartPanel(chart);
    }

    public static ChartPanel drawSeasonChart(Season season, int year, TemperatureData data) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        var validDates = data.getData().entrySet().stream()
                .filter(date -> Season.getSeason(date.getKey().getMonth()) == season && date.getKey().getYear() == year)
                .toList();
        validDates.forEach(date -> addTemperatureDataToDataset(dataset, date.getKey(), date.getValue()));

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Температура за " + season.getRussianTranslation() + " " + year,
                "Дата",
                "Температура",
                dataset
        );
        return new ChartPanel(chart);
    }

    public static ChartPanel drawFullChart(TemperatureData data) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (Map.Entry<LocalDate, Map<Double, Double>> entry : data.getData().entrySet()) {
            LocalDate date = entry.getKey();
            addTemperatureDataToDataset(dataset, date, entry.getValue());
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Полный график температуры",
                "Дата",
                "Температура",
                dataset
        );
        return new ChartPanel(chart);
    }

    private static void addTemperatureDataToDataset(XYSeriesCollection dataset, LocalDate date, Map<Double, Double> temperatureData) {
        if (date.getMonth().equals(Month.DECEMBER)) {
            date = date.minusYears(1);
        }

        XYSeries series = new XYSeries(date);

        for (Map.Entry<Double, Double> entry : temperatureData.entrySet()) {
            double depth = entry.getKey();
            double temperature = entry.getValue();
            series.add(depth, temperature);
        }

        dataset.addSeries(series);
    }
}
