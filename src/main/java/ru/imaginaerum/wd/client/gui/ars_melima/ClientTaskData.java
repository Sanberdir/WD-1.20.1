package ru.imaginaerum.wd.client.gui.ars_melima;

import java.util.HashMap;
import java.util.Map;

public class ClientTaskData {
    private static final Map<String, Map<String, Integer>> taskProgress = new HashMap<>();

    public static void updateTaskProgress(String learningChapterId, String taskId, int progress) {
        taskProgress.computeIfAbsent(learningChapterId, k -> new HashMap<>()).put(taskId, progress);
        System.out.println("[ArsMelima] Client task progress updated: " + learningChapterId + "/" + taskId + " = " + progress);
    }

    public static int getTaskProgress(String learningChapterId, String taskId) {
        // Если learningChapterId не найден, пробуем найти задачу в любой главе
        if (!taskProgress.containsKey(learningChapterId)) {
            // Поиск задачи во всех главах
            for (Map.Entry<String, Map<String, Integer>> entry : taskProgress.entrySet()) {
                if (entry.getValue().containsKey(taskId)) {
                    int progress = entry.getValue().get(taskId);
                    System.out.println("[ArsMelima] Found task in different chapter: " + entry.getKey() + "/" + taskId + " = " + progress);
                    return progress;
                }
            }
            System.out.println("[ArsMelima] No progress found for: " + learningChapterId + "/" + taskId);
            return 0;
        }

        Map<String, Integer> chapterProgress = taskProgress.get(learningChapterId);
        int progress = chapterProgress.getOrDefault(taskId, 0);
        System.out.println("[ArsMelima] Getting task progress: " + learningChapterId + "/" + taskId + " = " + progress);
        return progress;
    }

    public static void clearProgress() {
        taskProgress.clear();
    }

    public static void initProgress(Map<String, Map<String, Integer>> progressData) {
        taskProgress.clear();
        if (progressData != null) {
            taskProgress.putAll(progressData);
            System.out.println("[ArsMelima] Initialized task progress with " + progressData.size() + " chapters");
        }
    }
}