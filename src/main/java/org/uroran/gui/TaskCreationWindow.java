package org.uroran.gui;

import org.uroran.service.TaskManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Класс окна создания задачи (доработать).
 */
public class TaskCreationWindow extends JDialog {

    private final JTextField commandField;
    private final JTextField totalTimeField;
    private final JTextField segmentsField;
    private final JButton startButton;

    public TaskCreationWindow(Frame parent, String path, TaskManager taskManager) {
        super(parent, "Task Manager", true);

        setSize(400, 200);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Создать новую сессию"));

        add(mainPanel);

        // Верхняя панель для ввода команды
        JPanel commandPanel = new JPanel(new BorderLayout());
        JLabel commandLabel = new JLabel("Команда:  ");
        commandField = new JTextField("mqrun -np 1 -maxtime 30 " + path);
        commandPanel.add(commandLabel, BorderLayout.WEST);
        commandPanel.add(commandField, BorderLayout.CENTER);

        // Средняя панель для полей ввода времени
        JPanel timePanel = new JPanel(new GridLayout(2, 2, 10, 10));
        JLabel totalTimeLabel = new JLabel("Суммарное время (мин):  ");
        totalTimeField = new JTextField();
        JLabel segmentsLabel = new JLabel("Количество отрезков:  ");
        segmentsField = new JTextField();
        timePanel.add(totalTimeLabel);
        timePanel.add(totalTimeField);
        timePanel.add(segmentsLabel);
        timePanel.add(segmentsField);

        // Нижняя панель для кнопки
        JPanel buttonPanel = new JPanel();
        startButton = new JButton("Старт");
        startButton.setEnabled(false); // Кнопка выключена по умолчанию
        buttonPanel.add(startButton);

        mainPanel.add(commandPanel, BorderLayout.NORTH);
        mainPanel.add(timePanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        addInputListeners();

        startButton.addActionListener(_ -> {
            String command = commandField.getText();
            if (!totalTimeField.getText().isEmpty() && !segmentsField.getText().isEmpty()) {
                try {
                    int totalTime = Integer.parseInt(totalTimeField.getText());
                    int segments = Integer.parseInt(segmentsField.getText());
                    int segmentTime = totalTime / segments;

                    command = command.replaceFirst("-maxtime \\d+", "-maxtime " + segmentTime);

                    taskManager.addTask(command, segmentTime, segments);
                    dispose();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(TaskCreationWindow.this, "Введите корректные числа!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    throw new RuntimeException("Ошибка при отправке команды", e);
                }
            }
        });
    }

    /**
     * Добавляет слушателей для ввода данных, чтобы включить/выключить кнопку "Старт".
     */
    private void addInputListeners() {
        ActionListener inputListener = _ -> {
            boolean allFieldsFilled = !commandField.getText().isEmpty()
                    && !totalTimeField.getText().isEmpty()
                    && !segmentsField.getText().isEmpty();
            startButton.setEnabled(allFieldsFilled);
        };

        // Слушатели для изменения текста в полях
        commandField.getDocument().addDocumentListener(new SimpleDocumentListener(inputListener));
        totalTimeField.getDocument().addDocumentListener(new SimpleDocumentListener(inputListener));
        segmentsField.getDocument().addDocumentListener(new SimpleDocumentListener(inputListener));
    }

    /**
     * Вспомогательный класс для отслеживания изменений текста.
     */
    private record SimpleDocumentListener(ActionListener actionListener) implements javax.swing.event.DocumentListener {

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            actionListener.actionPerformed(null);
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            actionListener.actionPerformed(null);
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            actionListener.actionPerformed(null);
        }
    }
}
