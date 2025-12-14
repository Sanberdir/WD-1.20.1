package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class ServerTaskStorage {
    private static final String ROOT_TAG = "wd_task_progress";

    private static CompoundTag getRoot(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    /**
     * Получение прогресса с указанием главы
     */
    public static int getProgress(ServerPlayer player, String chapterId, String taskId) {
        if (player == null || chapterId == null || taskId == null) {
            return 0;
        }

        CompoundTag root = getRoot(player);

        // Проверяем существование chapter
        if (!root.contains(chapterId)) {
            return 0;
        }

        CompoundTag chapterTag = root.getCompound(chapterId);
        return chapterTag.getInt(taskId);
    }

    /**
     * Установка прогресса с указанием главы
     */
    public static void setProgress(ServerPlayer player, String chapterId, String taskId, int value) {
        if (player == null || chapterId == null || taskId == null) {
            return;
        }

        CompoundTag root = getRoot(player);
        CompoundTag chapterTag = root.contains(chapterId) ? root.getCompound(chapterId) : new CompoundTag();

        chapterTag.putInt(taskId, value);
        root.put(chapterId, chapterTag);
        player.getPersistentData().put(ROOT_TAG, root);
    }

    /**
     * Увеличение прогресса с указанием главы
     */
    public static int incrementProgress(ServerPlayer player, String chapterId, String taskId, int delta) {
        if (player == null || chapterId == null || taskId == null) {
            return 0;
        }

        int prev = getProgress(player, chapterId, taskId);
        int next = Math.max(0, prev + delta);
        setProgress(player, chapterId, taskId, next);

        return next;
    }

    /**
     * Получение ВСЕГО прогресса игрока (для синхронизации)
     */
    public static CompoundTag getAllProgress(ServerPlayer player) {
        if (player == null) return new CompoundTag();

        CompoundTag root = getRoot(player);
        return root.copy();
    }

    /**
     * Инициализация прогресса для главы (если не существует)
     */
    public static void initializeChapter(ServerPlayer player, String chapterId) {
        if (player == null || chapterId == null) return;

        CompoundTag root = getRoot(player);
        if (!root.contains(chapterId)) {
            root.put(chapterId, new CompoundTag());
            player.getPersistentData().put(ROOT_TAG, root);
        }
    }

    // -----------------------
    // Совместимые методы (старая схема) - УДАЛИТЬ ПОСЛЕ ОБНОВЛЕНИЯ
    // -----------------------

    /**
     * Совместимый getProgress(player, taskId) - УСТАРЕВШИЙ
     * @deprecated Используйте getProgress(player, chapterId, taskId)
     */
    @Deprecated
    public static int getProgress(ServerPlayer player, String taskId) {
        if (player == null || taskId == null) return 0;

        CompoundTag root = getRoot(player);

        // 1) плоский ключ
        if (root.contains(taskId)) {
            return root.getInt(taskId);
        }

        // 2) поиск по всем chapter-ключам
        for (String chapterId : root.getAllKeys()) {
            if (!root.get(chapterId).getClass().equals(CompoundTag.class)) continue;
            CompoundTag ch = root.getCompound(chapterId);
            if (ch.contains(taskId)) {
                return ch.getInt(taskId);
            }
        }

        return 0;
    }
}