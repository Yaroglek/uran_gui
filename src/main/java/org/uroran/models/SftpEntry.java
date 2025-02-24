package org.uroran.models;

import lombok.*;

/**
 * Класс, содержащий характеристики файлов (SFTP протокол)
 */
@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SftpEntry {
    private String name;
    private EntryType entryType;
    private String mTime;

    public enum EntryType {
        DIRECTORY,
        FILE,
        LINK;

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
