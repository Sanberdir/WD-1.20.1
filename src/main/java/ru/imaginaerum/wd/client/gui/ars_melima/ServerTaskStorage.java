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

    // Оригинальные методы (chapterId + taskId)
    public static int getProgress(ServerPlayer player, String chapterId, String taskId) {
        if (player == null || chapterId == null || taskId == null) return 0;
        CompoundTag root = getRoot(player);
        if (!root.contains(chapterId)) return 0;
        CompoundTag ch = root.getCompound(chapterId);
        return ch.getInt(taskId);
    }

    public static void setProgress(ServerPlayer player, String chapterId, String taskId, int value) {
        if (player == null || chapterId == null || taskId == null) return;
        CompoundTag root = getRoot(player);
        CompoundTag ch = root.contains(chapterId) ? root.getCompound(chapterId) : new CompoundTag();
        ch.putInt(taskId, value);
        root.put(chapterId, ch);
        player.getPersistentData().put(ROOT_TAG, root);
    }

    public static int incrementProgress(ServerPlayer player, String chapterId, String taskId, int delta) {
        int prev = getProgress(player, chapterId, taskId);
        int next = Math.max(0, prev + delta);
        setProgress(player, chapterId, taskId, next);
        return next;
    }

    // -----------------------
    // Compatibility overloads
    // -----------------------

    /**
     * Совместимый getProgress(player, taskId)
     * Сначала пытается взять плоский ключ root.getInt(taskId),
     * затем ищет в каждой chapter-compound (если задача там хранится).
     */
    public static int getProgress(ServerPlayer player, String taskId) {
        if (player == null || taskId == null) return 0;
        CompoundTag root = getRoot(player);

        // 1) плоский ключ
        if (root.contains(taskId)) {
            return root.getInt(taskId);
        }

        // 2) поиск по всем chapter-ключам
        for (String key : root.getAllKeys()) {
            // пропускаем плоские int-ключи, ищем только compound'ы
            if (!root.get(key).getClass().equals(CompoundTag.class)) continue;
            CompoundTag ch = root.getCompound(key);
            if (ch.contains(taskId)) return ch.getInt(taskId);
        }

        return 0;
    }

    /**
     * Совместимый setProgress(player, taskId, value)
     * Если taskId уже есть внутри какого-то chapter-compound — обновляем там.
     * Иначе — записываем как плоский ключ root.putInt(taskId, value).
     */
    public static void setProgress(ServerPlayer player, String taskId, int value) {
        if (player == null || taskId == null) return;
        CompoundTag root = getRoot(player);

        // 1) найти в каких-то chapter-compound — если найдено, обновить там
        for (String key : root.getAllKeys()) {
            if (!root.get(key).getClass().equals(CompoundTag.class)) continue;
            CompoundTag ch = root.getCompound(key);
            if (ch.contains(taskId)) {
                ch.putInt(taskId, value);
                root.put(key, ch);
                player.getPersistentData().put(ROOT_TAG, root);
                return;
            }
        }

        // 2) иначе — плоская запись
        root.putInt(taskId, value);
        player.getPersistentData().put(ROOT_TAG, root);
    }

    public static int incrementProgress(ServerPlayer player, String taskId, int delta) {
        int prev = getProgress(player, taskId);
        int next = Math.max(0, prev + delta);
        setProgress(player, taskId, next);
        return next;
    }

    /**
     * Совместимый save — в простейшей реализации no-op,
     * т.к. player.getPersistentData() автоматически сохраняется сервером.
     * Оставлен для совместимости с кодом, который вызывает ServerTaskStorage.save(player).
     */
    public static void save(ServerPlayer player) {
        // intentionally empty — persistentData сохраняется сервером
    }
}
