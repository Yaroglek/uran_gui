package org.uroran.service.exporters;

/**
 * Фабрика экспортеров
 */
public class ChartExporterFactory {
    public static ChartExporter getExporter(Format format) {
        return switch (format) {
            case Format.PNG -> new PngExporter();
            case Format.XLSX -> new XlsxExporter();
        };
    }
}
