package org.uroran.gui;

import org.jfree.chart.ChartPanel;
import org.uroran.models.Season;
import org.uroran.models.TemperatureData;
import org.uroran.service.exporters.ChartExporter;
import org.uroran.service.exporters.ChartExporterFactory;
import org.uroran.service.exporters.Format;
import org.uroran.util.ChartDrawer;
import org.uroran.util.ChartUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;

/**
 * Класс окна для просмотра температурных профилей.
 */
public class ChartWindow extends JFrame {
    private final TemperatureData temperatureData;
    private ChartPanel currentChart;
    private final JPanel chartPanel;

    public ChartWindow(TemperatureData temperatureData) {
        this.temperatureData = temperatureData;

        setTitle("Скважина №" + temperatureData.getPointNumber());
        setSize(1000, 600);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        JPanel managingPanel = createManagingPanel();
        add(managingPanel, BorderLayout.WEST);

        chartPanel = new JPanel(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);
    }

    /**
     * Метод для создания панели управления графиком
     *
     * @return - панель
     */
    private JPanel createManagingPanel() {
        JPanel managingPanel = new JPanel(new BorderLayout());
        managingPanel.setPreferredSize(new Dimension(350, this.getHeight()));
        managingPanel.setBorder(BorderFactory.createSoftBevelBorder(0));

        //Панель с выпадающими списками
        managingPanel.add(getTopManagingPanel());

        // Панель с кнопками экспорта
        managingPanel.add(getBottomManagingPanel(), BorderLayout.SOUTH);

        return managingPanel;
    }

    /**
     * Метод для создания верхней части панели управления (с выбором года, месяца, сезона)
     *
     * @return - панель
     */
    private JPanel getTopManagingPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<String> chartTypes = new JComboBox<>(new String[]{"", "Месяц", "Сезон", "Всё"});
        JComboBox<String> valueSelector = new JComboBox<>();
        JComboBox<String> yearSelector = new JComboBox<>();
        valueSelector.setEnabled(false);
        yearSelector.setEnabled(false);

        chartTypes.addActionListener(_ -> listenChartTypes(chartTypes, yearSelector, valueSelector));
        yearSelector.addActionListener(_ -> listenYearSelector(chartTypes, yearSelector, valueSelector));
        valueSelector.addActionListener(_ -> listenValueSelector(chartTypes, yearSelector, valueSelector));

        Dimension comboBoxSize = new Dimension(150, 30);

        topPanel.add(createComboBoxPanel(chartTypes, new JLabel("Выберите тип:"), comboBoxSize));
        topPanel.add(createComboBoxPanel(yearSelector, new JLabel("Выберите год:"), comboBoxSize));
        topPanel.add(createComboBoxPanel(valueSelector, new JLabel("Выберите значение:"), comboBoxSize));
        return topPanel;
    }

    /**
     * Метод для создания нижней части панели управления (кнопки экспорта)
     *
     * @return - панель
     */
    private JPanel getBottomManagingPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton exportImageButton = new JButton("Экспорт графика");
        JButton exportXlsxButton = new JButton("Экспорт данных");

        Dimension buttonSize = new Dimension(150, 30); // Размер кнопок
        exportImageButton.setPreferredSize(buttonSize);
        exportXlsxButton.setPreferredSize(buttonSize);

        exportImageButton.addActionListener(_ -> exportChartAsPng());
        exportXlsxButton.addActionListener(_ -> exportChartAsXLSX());

        bottomPanel.add(exportImageButton);
        bottomPanel.add(exportXlsxButton);
        return bottomPanel;
    }

    /**
     * Метод для создания универсальной панели с комбобоксом.
     *
     * @param comboBox          - комбобокс
     * @param label             - надпись
     * @param comboBoxDimension - размер
     * @return - панель
     */
    private JPanel createComboBoxPanel(JComboBox<String> comboBox, JLabel label, Dimension comboBoxDimension) {
        JPanel topPanelElement = new JPanel(new FlowLayout(FlowLayout.TRAILING));

        comboBox.setPreferredSize(comboBoxDimension);
        topPanelElement.add(label);
        topPanelElement.add(comboBox);

        return topPanelElement;
    }

    /**
     * Слушатель для комбобокса по изменению типа графика
     *
     * @param chartTypes    - типы графиков
     * @param yearSelector  - селектор годов
     * @param valueSelector - селектор значений (месяцы, сезоны)
     */
    private void listenChartTypes(JComboBox<String> chartTypes, JComboBox<String> yearSelector, JComboBox<String> valueSelector) {
        String selectedType = (String) chartTypes.getSelectedItem();
        yearSelector.removeAllItems();
        valueSelector.removeAllItems();
        valueSelector.setEnabled(false);

        if ("Месяц".equals(selectedType) || "Сезон".equals(selectedType)) {
            Integer[] availableYears = ChartUtils.getAvailableYears(this.temperatureData);
            yearSelector.addItem("");
            for (int year : availableYears) {
                yearSelector.addItem(String.valueOf(year));
            }
            yearSelector.setEnabled(true);
        } else if ("Все".equals(selectedType)) {
            updateChart(ChartDrawer.drawFullChart(temperatureData));
            yearSelector.setEnabled(false);
        }
    }

    /**
     * Слушатель для комбобокса по годам
     *
     * @param chartTypes    - типы графиков
     * @param yearSelector  - селектор годов
     * @param valueSelector - селектор значений (месяцы, сезоны)
     */
    private void listenYearSelector(JComboBox<String> chartTypes, JComboBox<String> yearSelector, JComboBox<String> valueSelector) {
        if (!yearSelector.isEnabled()) {
            return;
        }

        String selectedYear = (String) yearSelector.getSelectedItem();
        String selectedType = (String) chartTypes.getSelectedItem();

        valueSelector.removeAllItems();
        valueSelector.setEnabled(false);

        if (selectedYear == null || selectedYear.isEmpty()) {
            return;
        }

        int year = Integer.parseInt(selectedYear);

        if ("Месяц".equals(selectedType)) {
            valueSelector.addItem("");
            for (Month month : ChartUtils.getAvailableMonthsForYear(this.temperatureData, year)) {
                valueSelector.addItem(month.toString());
            }
            valueSelector.setEnabled(true);
        } else if ("Сезон".equals(selectedType)) {
            valueSelector.addItem("");
            for (Season season : ChartUtils.getAvailableSeasonsForYear(this.temperatureData, year)) {
                valueSelector.addItem(season.toString());
            }
            valueSelector.setEnabled(true);
        }
    }

    /**
     * Слушатель для комбобокса по значениям (месяцы, сезоны)
     *
     * @param chartTypes    - типы графиков
     * @param yearSelector  - селектор годов
     * @param valueSelector - селектор значений (месяцы, сезоны)
     */
    private void listenValueSelector(JComboBox<String> chartTypes, JComboBox<String> yearSelector, JComboBox<String> valueSelector) {
        if (!valueSelector.isEnabled()) {
            return;
        }

        String selectedType = (String) chartTypes.getSelectedItem();
        String selectedValue = (String) valueSelector.getSelectedItem();
        String selectedYear = (String) yearSelector.getSelectedItem();

        if (selectedValue == null || selectedValue.isEmpty() || selectedYear == null || selectedYear.isEmpty()) {
            return;
        }

        int year = Integer.parseInt(selectedYear);

        switch (selectedType) {
            case "Месяц": {
                Month selectedMonth = Month.valueOf(selectedValue.toUpperCase());
                updateChart(ChartDrawer.drawMonthChart(LocalDate.of(year, selectedMonth, 1), temperatureData));
                break;
            }

            case "Сезон": {
                Season selectedSeason = Season.valueOf(selectedValue.toUpperCase());
                updateChart(ChartDrawer.drawSeasonChart(selectedSeason, year, temperatureData));
                break;
            }

            case "Всё": {
                updateChart(ChartDrawer.drawFullChart(temperatureData));
                break;
            }

            case null, default:
                break;
        }
    }

    /**
     * Метод для обновления панели с графиком.
     *
     * @param newChartPanel - новая панель с графиком.
     */
    private void updateChart(ChartPanel newChartPanel) {
        currentChart = newChartPanel;

        chartPanel.removeAll();
        chartPanel.add(newChartPanel, BorderLayout.CENTER);
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    /**
     * Метод для экспорта в виде картинки
     */
    private void exportChartAsPng() {
        exportChart(Format.PNG);
    }

    /**
     * Метод для экспорта в виде xlsx таблицы
     */
    private void exportChartAsXLSX() {
        exportChart(Format.XLSX);
    }

    /**
     * Метод для экспорта графика
     */
    private void exportChart(Format format) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить");
        fileChooser.setFileFilter(new FileNameExtensionFilter(format.getName().toUpperCase() + " файл", format.getName()));

        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            ChartExporter exporter = ChartExporterFactory.getExporter(format);

            try {
                if (format == Format.PNG) {
                    exporter.export(currentChart.getChart(), fileToSave);
                } else if (format == Format.XLSX) {
                    exporter.export(temperatureData, fileToSave);
                }
                JOptionPane.showMessageDialog(null, "Экспорт выполнен успешно: " + fileToSave.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Ошибка экспорта: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
