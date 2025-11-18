package ru.imaginaerum.wd.client.gui.ars_melima;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientTaskData {
    private static final Map<String, Map<String, Integer>> taskProgress = new HashMap<>();

    // --- Добавлено: список слушателей ---
    private static final List<TaskProgressListener> listeners = new CopyOnWriteArrayList<>();

    private static void notifyUpdate(String chapterId, String taskId, int progress) {
        for (TaskProgressListener l : listeners) {
            try { l.onTaskProgressUpdated(chapterId, taskId, progress); }
            catch (Throwable t) { }
        }
    }

    private static void notifyInit(Map<String, Map<String, Integer>> allProgress) {
        for (TaskProgressListener l : listeners) {
            try { l.onAllProgressInitialized(allProgress); }
            catch (Throwable t) { }
        }
    }

    // --- Основная логика обновлений ---
    public static void updateTaskProgress(String learningChapterId, String taskId, int progress) {
        taskProgress.computeIfAbsent(learningChapterId, k -> new HashMap<>()).put(taskId, progress);

        // --- Новое: уведомляем слушателей ---
        notifyUpdate(learningChapterId, taskId, progress);
    }

    public static int getTaskProgress(String learningChapterId, String taskId) {
        if (!taskProgress.containsKey(learningChapterId)) {
            return 0;
        }
        Map<String, Integer> chapterProgress = taskProgress.get(learningChapterId);
        return chapterProgress.getOrDefault(taskId, 0);
    }

    /**
     * Инициализация всего прогресса (при входе в игру)
     */
    public static void initProgress(Map<String, Map<String, Integer>> progressData) {
        taskProgress.clear();
        if (progressData != null) {
            taskProgress.putAll(progressData);
        }

        // --- Новое: уведомляем всех, что пришли полные данные ---
        notifyInit(progressData);
    }
}