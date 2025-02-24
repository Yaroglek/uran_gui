package org.uroran.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Класс для сохранения и чтения настроек программы.
 */
public class SettingsManager {
    private static final Path SETTINGS_FILE = Paths.get(System.getProperty("user.home"), "settings.properties");
    private final Properties properties;

    public SettingsManager() {
        this.properties = new Properties();
        try {
            if (!Files.exists(SETTINGS_FILE)) {
                Files.createDirectories(SETTINGS_FILE.getParent());
                Files.createFile(SETTINGS_FILE);
            }
            try (InputStream inputStream = Files.newInputStream(SETTINGS_FILE)) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при инициализации файла настроек", e);
        }
    }

    /**
     * Сохраняет текущие настройки в файл.
     */
    public void saveSettings() {
        try (OutputStream outputStream = Files.newOutputStream(SETTINGS_FILE)) {
            properties.store(outputStream, "Application Settings");
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении настроек", e);
        }
    }

    /**
     * Получает значение настройки по ключу.
     * @param key - ключ
     * @return значение
     */
    public String getSetting(String key) {
        return properties.getProperty(key);
    }

    /**
     * Получает значение настройки по ключу или значение по умолчанию, если ключ отсутствует.
     * @param key          ключ настройки.
     * @param defaultValue значение по умолчанию.
     * @return значение настройки или {@code defaultValue}, если ключ отсутствует.
     */
    public String getSetting(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Устанавливает значение настройки и сохраняет изменения.
     * @param key - ключ
     * @param value - значение
     */
    public void setSetting(String key, String value) {
        properties.setProperty(key, value);
        saveSettings();
    }
}
