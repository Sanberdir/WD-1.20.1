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
            catch (Throwable t) { System.err.println("[ArsMelima] Listener error (update): " + t.getMessage()); }
        }
    }

    private static void notifyInit(Map<String, Map<String, Integer>> allProgress) {
        for (TaskProgressListener l : listeners) {
            try { l.onAllProgressInitialized(allProgress); }
            catch (Throwable t) { System.err.println("[ArsMelima] Listener error (init): " + t.getMessage()); }
        }
    }

    // --- Основная логика обновлений ---
    public static void updateTaskProgress(String learningChapterId, String taskId, int progress) {
        System.out.println("[ArsMelima] ClientTaskData UPDATE: " + learningChapterId + "/" + taskId + " = " + progress);

        taskProgress.computeIfAbsent(learningChapterId, k -> new HashMap<>()).put(taskId, progress);

        // Отладочный вывод текущего состояния
        System.out.println("[ArsMelima] ClientTaskData current state:");
        for (String chapter : taskProgress.keySet()) {
            Map<String, Integer> chapterTasks = taskProgress.get(chapter);
            for (String task : chapterTasks.keySet()) {
                System.out.println("[ArsMelima]   " + chapter + "/" + task + " = " + chapterTasks.get(task));
            }
        }

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
        System.out.println("[ArsMelima] ClientTaskData INIT with " + (progressData != null ? progressData.size() : 0) + " chapters");

        taskProgress.clear();
        if (progressData != null) {
            taskProgress.putAll(progressData);

            for (Map.Entry<String, Map<String, Integer>> chapterEntry : progressData.entrySet()) {
                String chapterId = chapterEntry.getKey();
                Map<String, Integer> tasks = chapterEntry.getValue();
                System.out.println("[ArsMelima] ClientTaskData Chapter '" + chapterId + "' has " + tasks.size() + " tasks:");
                for (Map.Entry<String, Integer> taskEntry : tasks.entrySet()) {
                    System.out.println("[ArsMelima]   " + taskEntry.getKey() + " = " + taskEntry.getValue());
                }
            }
        }

        // --- Новое: уведомляем всех, что пришли полные данные ---
        notifyInit(progressData);
    }

    public static void clearProgress() {
        taskProgress.clear();
        System.out.println("[ArsMelima] ClientTaskData cleared");
    }
}
