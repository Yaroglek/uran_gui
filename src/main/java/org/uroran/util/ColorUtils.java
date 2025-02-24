package org.uroran.util;

import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс для раскрашивания текста по ANSI
 */
public class ColorUtils {
    private ColorUtils() {
    }

    /**
     * Метод для добавления текста с указанным цветом.
     */
    public static void appendColoredText(StyledDocument document, String text, Color color) throws BadLocationException {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        document.insertString(document.getLength(), text, attributes);

    }

    /**
     * Метод для парсинга и отображения текста с ANSI-кодами.
     */
    public static void appendAnsiColoredText(StyledDocument document, String text) throws BadLocationException {
        // Регулярное выражение для ANSI-кодов
        Pattern ansiPattern = Pattern.compile("\u001B\\[(\\d+;?)*m");
        Matcher matcher = ansiPattern.matcher(text);
        int lastEnd = 0;

        SimpleAttributeSet attributes = new SimpleAttributeSet();

        while (matcher.find()) {
            // Добавляем текст до ANSI-кода
            if (matcher.start() > lastEnd) {
                document.insertString(document.getLength(), text.substring(lastEnd, matcher.start()), attributes);
            }

            // Обновляем стиль текста в зависимости от ANSI-кода
            String ansiCode = matcher.group();
            updateAttributesForAnsiCode(ansiCode, attributes);

            lastEnd = matcher.end();
        }

        // Добавляем оставшийся текст
        if (lastEnd < text.length()) {
                document.insertString(document.getLength(), text.substring(lastEnd), attributes);
        }

    }

    /**
     * Метод для получения цвета по ANSI коду.
     */
    public static void updateAttributesForAnsiCode(String ansiCode, SimpleAttributeSet attributes) {
        if (ansiCode.contains("31")) {
            StyleConstants.setForeground(attributes, Color.RED);
        } else if (ansiCode.contains("32")) {
            StyleConstants.setForeground(attributes, Color.GREEN);
        } else if (ansiCode.contains("33")) {
            StyleConstants.setForeground(attributes, Color.YELLOW);
        } else if (ansiCode.contains("34")) {
            StyleConstants.setForeground(attributes, Color.BLUE);
        } else if (ansiCode.contains("35")) {
            StyleConstants.setForeground(attributes, Color.MAGENTA);
        } else if (ansiCode.contains("36")) {
            StyleConstants.setForeground(attributes, Color.CYAN);
        } else if (ansiCode.contains("0")) {
            StyleConstants.setForeground(attributes, Color.BLACK);
        }
    }
}
