package org.uroran.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * Класс Task представляет задачу, которую нужно выполнить.
 */
@Getter
@Data
@AllArgsConstructor
public class Task {
    private final String command;
    private final int intervalMinutes;
    private final int maxRetries;
    private int currentRetries;
    private String jobId;

    public Task(String command, int intervalMinutes, int maxRetries) {
        this.command = command;
        this.intervalMinutes = intervalMinutes;
        this.maxRetries = maxRetries;
        this.currentRetries = 0;
    }

    public void incrementRetries() {
        this.currentRetries++;
    }

    public boolean isComplete() {
        return currentRetries >= maxRetries;
    }
}
