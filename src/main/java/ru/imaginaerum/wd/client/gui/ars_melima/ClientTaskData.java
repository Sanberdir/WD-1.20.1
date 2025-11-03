package ru.imaginaerum.wd.client.gui.ars_melima;

import java.util.HashMap;
import java.util.Map;

public class ClientTaskData {
    private static final Map<String, Map<String, Integer>> taskProgress = new HashMap<>();

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
    }

    public static int getTaskProgress(String learningChapterId, String taskId) {
        System.out.println("[ArsMelima] ClientTaskData GET: " + learningChapterId + "/" + taskId);

        // Проверяем существование главы
        if (!taskProgress.containsKey(learningChapterId)) {
            System.out.println("[ArsMelima] ClientTaskData: Chapter not found: " + learningChapterId);
            System.out.println("[ArsMelima] ClientTaskData: Available chapters: " + taskProgress.keySet());
            return 0;
        }

        Map<String, Integer> chapterProgress = taskProgress.get(learningChapterId);
        int progress = chapterProgress.getOrDefault(taskId, 0);

        System.out.println("[ArsMelima] ClientTaskData: " + learningChapterId + "/" + taskId + " = " + progress);
        return progress;
    }

    /**
     * Инициализация всего прогресса (при входе в игру)
     */
    public static void initProgress(Map<String, Map<String, Integer>> progressData) {
        System.out.println("[ArsMelima] ClientTaskData INIT with " + (progressData != null ? progressData.size() : 0) + " chapters");

        taskProgress.clear();
        if (progressData != null) {
            taskProgress.putAll(progressData);

            // Детальный вывод принятых данных
            for (Map.Entry<String, Map<String, Integer>> chapterEntry : progressData.entrySet()) {
                String chapterId = chapterEntry.getKey();
                Map<String, Integer> tasks = chapterEntry.getValue();
                System.out.println("[ArsMelima] ClientTaskData Chapter '" + chapterId + "' has " + tasks.size() + " tasks:");
                for (Map.Entry<String, Integer> taskEntry : tasks.entrySet()) {
                    System.out.println("[ArsMelima]   " + taskEntry.getKey() + " = " + taskEntry.getValue());
                }
            }
        } else {
            System.out.println("[ArsMelima] ClientTaskData WARNING: initProgress called with null data");
        }
    }

    public static void clearProgress() {
        taskProgress.clear();
        System.out.println("[ArsMelima] ClientTaskData cleared");
    }
}