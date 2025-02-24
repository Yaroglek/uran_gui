package org.uroran.service.exporters;

import lombok.Getter;

/**
 * Enum для форматов файлов
 */
@Getter
public enum Format {
    PNG("png"),
    XLSX("xlsx");

    private final String name;

    Format(String name) {
        this.name = name;
    }
}
