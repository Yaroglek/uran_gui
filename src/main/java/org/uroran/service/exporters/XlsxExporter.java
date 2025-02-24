package org.uroran.service.exporters;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.uroran.models.TemperatureData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

/**
 * Экспорт файлов в XLSX формат (Apache POI)
 */
public class XlsxExporter implements ChartExporter{
    @Override
    public void export(Object data, File fileToSave) throws IOException {
        if (!(data instanceof TemperatureData)) {
            throw new IllegalArgumentException("Неверный тип данных для экспорта в XLSX.");
        }

        // Проверяем и добавляем расширение файла, если нужно
        if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".xlsx");
        }

        // Создаем workbook и sheet
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Temperature Data");

            // Создаем стили для ячеек (опционально)
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Создаем заголовки: первый столбец — даты, первая строка — глубины
            Map<LocalDate, Map<Double, Double>> temperatureData = ((TemperatureData) data).getData();
            if (temperatureData.isEmpty()) {
                throw new IllegalArgumentException("Нет данных для экспорта");
            }

            Row headerRow = sheet.createRow(0); // Первая строка для заголовков
            headerRow.createCell(0).setCellValue("Date"); // Первый столбец для дат

            // Собираем все глубины
            Map.Entry<LocalDate, Map<Double, Double>> firstEntry = temperatureData.entrySet().iterator().next();
            Map<Double, Double> depths = firstEntry.getValue();
            if (depths == null || depths.isEmpty()) {
                throw new IllegalArgumentException("Нет данных о глубинах");
            }

            int colIndex = 1;
            for (Double depth : depths.keySet()) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(depth + " m"); // Добавляем заголовки глубин
                cell.setCellStyle(headerStyle);
            }

            // Добавляем строки с данными
            int rowIndex = 1;
            for (Map.Entry<LocalDate, Map<Double, Double>> entry : temperatureData.entrySet()) {
                LocalDate date = entry.getKey();
                Map<Double, Double> temperatureMap = entry.getValue();

                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(date.toString()); // Первая ячейка — дата

                colIndex = 1;
                for (Double depth : depths.keySet()) {
                    Double temperature = temperatureMap.getOrDefault(depth, null);
                    if (temperature != null) {
                        row.createCell(colIndex).setCellValue(temperature);
                    }
                    colIndex++;
                }
            }

            // Авторазмер столбцов
            for (int i = 0; i <= depths.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // Сохраняем файл
            try (FileOutputStream outputStream = new FileOutputStream(fileToSave)) {
                workbook.write(outputStream);
            }
        }
    }
}
