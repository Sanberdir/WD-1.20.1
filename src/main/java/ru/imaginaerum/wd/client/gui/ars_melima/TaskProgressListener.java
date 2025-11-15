package ru.imaginaerum.wd.client.gui.ars_melima;

import java.util.Map;

public interface  TaskProgressListener {
    /** Точечное обновление: конкретная задача в learning-chapter */
    void onTaskProgressUpdated(String learningChapterId, String taskId, int progress);

    /** Полная инициализация прогресса (при входе) */
    void onAllProgressInitialized(Map<String, Map<String, Integer>> allProgress);
}
