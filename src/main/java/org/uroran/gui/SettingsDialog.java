package org.uroran.gui;

import org.uroran.service.SettingsManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Класс окна-диалога с настройками.
 */
public class SettingsDialog extends JDialog {
    private final SettingsManager settingsManager;
    private JCheckBox syncDirectoriesCheckBox;

    public SettingsDialog(JFrame parent, SettingsManager settingsManager, Runnable onCloseCallback) {
        super(parent, "Настройки", true);
        this.settingsManager = settingsManager;

        initUI();
        loadSettings();
        pack();
        setLocationRelativeTo(parent);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
                dispose();
            }
        });
    }

    /**
     * Метод для инициализации элементов UI.
     */
    private void initUI() {
        // Панель с настройками
        JPanel settingsPanel = new JPanel(new GridLayout(1, 1));
        syncDirectoriesCheckBox = new JCheckBox("Синхронизация перехода по директориям");
        settingsPanel.add(syncDirectoriesCheckBox);

        // Кнопки управления
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Сохранить");
        JButton cancelButton = new JButton("Отмена");

        saveButton.addActionListener(_ -> {
            saveSettings();
            dispose();
        });

        cancelButton.addActionListener(_ -> dispose()); // Закрыть окно без сохранения

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        setLayout(new BorderLayout());
        add(settingsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Загружает текущие настройки в интерфейс.
     */
    private void loadSettings() {
        boolean isSyncEnabled = Boolean.parseBoolean(settingsManager.getSetting("syncDirectories", "true"));
        syncDirectoriesCheckBox.setSelected(isSyncEnabled);
    }

    /**
     * Сохраняет изменения настроек.
     */
    private void saveSettings() {
        boolean isSyncEnabled = syncDirectoriesCheckBox.isSelected();
        settingsManager.setSetting("syncDirectories", String.valueOf(isSyncEnabled));
    }
}
