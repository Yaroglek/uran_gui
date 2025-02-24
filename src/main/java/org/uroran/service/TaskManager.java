package org.uroran.service;

import lombok.Getter;
import org.uroran.models.Task;

import java.util.*;
import java.util.concurrent.*;

/**
 * Класс-менеджер для отслеживания выполнения задач и их продления.
 */
public class TaskManager {
    private static final String TASK_LIST_COMMAND = "mps";

    @Getter
    private final List<Task> tasks = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    private final SshService sshService;

    public TaskManager(SshService sshService) {
        this.sshService = sshService;
    }

    /**
     * Добавить новую задачу в TaskManager.
     */
    public void addTask(String command, int intervalMinutes, int maxRetries) {
        Task task = new Task(command, intervalMinutes, maxRetries);
        tasks.add(task);
        startTask(task);
    }

    /**
     * Запуск задачи.
     */
    private void startTask(Task task) {
        try {
            String submitOutput = sshService.sendCommand(task.getCommand());
            task.incrementRetries();
            task.setJobId(parseJobId(submitOutput));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Task started: " + task.getJobId());

        scheduler.schedule(() -> monitorTask(task), task.getIntervalMinutes(), TimeUnit.MINUTES);
    }

    /**
     * Метод для парсинга JobId
     * @param submitOutput - входная строка
     * @return - ID
     */
    private String parseJobId(String submitOutput) {
        String[] lines = submitOutput.split("\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Submitted batch job")) {
                String[] tokens = line.split("\\s+");
                return tokens[tokens.length - 1];
            }
        }

        return null;
    }

    /**
     * Мониторинг состояния задачи.
     */
    private void monitorTask(Task task) {
        String mpsOutput;
        try {
            mpsOutput = sshService.sendCommand(TASK_LIST_COMMAND);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<String> runningJobs = parseRunningJobs(mpsOutput);

        // Если задача завершена (JobID отсутствует в списке), запускаем следующий этап
        if (!runningJobs.contains(task.getJobId())) {
            if (!task.isComplete()) {
                task.incrementRetries();
                try {
                    String submitOutput = sshService.sendCommand(task.getCommand());
                    task.setJobId(parseJobId(submitOutput));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Task retried: " + task.getCommand()); // Отладка
                scheduler.schedule(() -> monitorTask(task), task.getIntervalMinutes(), TimeUnit.MINUTES);
            } else {
                System.out.println("Task complete: " + task.getCommand()); // Отладка
                tasks.remove(task);
            }
        } else {
            // Задача еще выполняется, проверяем снова через небольшой интервал
            System.out.println("Task still running, checking again: " + task.getCommand()); // Отладка
            scheduler.schedule(() -> monitorTask(task), 10, TimeUnit.SECONDS);
        }
    }

    /**
     * Парсинг вывода команды mps, чтобы получить список JobID.
     */
    private List<String> parseRunningJobs(String mpsOutput) {
        List<String> jobIds = new ArrayList<>();
        String[] lines = mpsOutput.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.matches("^\\d+\\s+.*")) { // Строка начинается с JobID
                String[] tokens = line.split("\\s+");
                jobIds.add(tokens[0]); // Первый элемент — JobID
            }
        }
        return jobIds;
    }

    /**
     * Завершение работы TaskManager.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}

