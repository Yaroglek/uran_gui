package org.uroran.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;

/**
 * Класс окна-диалога для редактирования файлов (vim/nano).
 */
public class TextEditorDialog extends JDialog {
    private final JTextArea textArea;
    private final String filePath;
    private final String originalContent;

    public TextEditorDialog(JFrame parent, String filePath, Runnable onCloseCallback) {
        super(parent, "Редактор текста - " + filePath, true);
        this.filePath = filePath;

        originalContent = readFileContent(filePath);

        textArea = new JTextArea(originalContent);
        JScrollPane scrollPane = new JScrollPane(textArea);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(parent);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
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
     * Метод для чтения содержимого файла
     * @param path - путь к файлу
     * @return - содержимое
     */
    private String readFileContent(String path) {
        try {
            return Files.readString(Paths.get(path));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Ошибка чтения файла: " + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return "";
        }
    }

    /**
     * Метод для записи во временный файл.
     * @param path - путь к файлу
     * @param content - содержимое
     */
    private void writeFileContent(String path, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(content);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Ошибка сохранения файла: " + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Метод для подтверждения сохранения содержимого.
     */
    private void handleWindowClosing() {
        String currentContent = textArea.getText();

        if (!currentContent.equals(originalContent)) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Сохранить изменения перед закрытием?",
                    "Сохранение файла",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                writeFileContent(filePath, currentContent);
                this.dispose();
            } else if (choice == JOptionPane.NO_OPTION) {
                this.dispose();
            }
        } else {
            this.dispose();
        }
    }
}
