package org.uroran.service.exporters;

import org.jfree.chart.JFreeChart;

import java.io.File;
import java.io.IOException;

/**
 * PNG экспорт графиков
 */
public class PngExporter implements ChartExporter {
    @Override
    public void export(Object chart, File fileToSave) throws IOException {
        if (!(chart instanceof JFreeChart)) {
            throw new IllegalArgumentException("Неверный тип данных для экспорта в PNG.");
        }

        if (!fileToSave.getName().toLowerCase().endsWith(".png")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
        }

        org.jfree.chart.ChartUtils.saveChartAsPNG(fileToSave, (JFreeChart) chart, 800, 600);
    }
}
