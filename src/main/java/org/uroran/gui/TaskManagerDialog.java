package org.uroran.gui;

import org.uroran.models.Task;
import org.uroran.service.TaskManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Класс окна-менеджера задач (доработать).
 */
public class TaskManagerDialog extends JDialog {

    private final TaskManager taskManager;
    private final DefaultListModel<String> taskListModel;
    private final JList<String> taskList;

    public TaskManagerDialog(Frame parent, TaskManager taskManager) {
        super(parent, "Список задач", true);
        this.taskManager = taskManager;

        // Инициализация окна
        setSize(400, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);

        // Основная панель
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        add(mainPanel);

        // Верхняя панель с заголовком
        JLabel titleLabel = new JLabel("Текущие задачи:");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Модель и список задач
        taskListModel = new DefaultListModel<>();
        taskList = new JList<>(taskListModel);
        JScrollPane scrollPane = new JScrollPane(taskList);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Нижняя панель с кнопкой
        JPanel buttonPanel = new JPanel();
        JButton refreshButton = new JButton("Обновить");
        buttonPanel.add(refreshButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Добавляем обработчик для кнопки "Обновить"
        refreshButton.addActionListener(_ -> updateTaskList());

        // Изначально заполняем список задач
        updateTaskList();
        pack();
    }

    /**
     * Метод для обновления списка задач в окне.
     */
    private void updateTaskList() {
        // Получаем обновлённый список задач
        List<Task> tasks = taskManager.getTasks();

        // Очищаем модель и добавляем новые задачи
        taskListModel.clear();
        for (Task task : tasks) {
            taskListModel.addElement(formatTask(task));
        }
    }

    /**
     * Форматирует задачу для отображения в списке.
     * @param task Задача
     * @return Строковое представление задачи
     */
    private String formatTask(Task task) {
        return String.format("ID: %s, Общее время: %d мин, Перезапусков было: %s, Перезапусков всего %s",
                task.getJobId(),
                task.getIntervalMinutes(),
                task.getCurrentRetries(),
                task.getMaxRetries());
    }
}
