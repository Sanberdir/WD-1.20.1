package ru.imaginaerum.wd.client.gui.ars_melima;


import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.*;


public class TaskManager {
    // chapterId -> tasks
    private static final Map<String, List<Task>> chapterTasks = new HashMap<>();
    // itemId -> list of tasks (reverse index)
    private static final Map<String, List<Task>> itemIndex = new HashMap<>();

    public static synchronized List<Task> getTasksForChapter(MinecraftServer server, String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) return Collections.emptyList();
        if (chapterTasks.containsKey(chapterId)) {
            System.out.println("[ArsMelima] Returning cached tasks for chapter: " + chapterId + " (" + chapterTasks.get(chapterId).size() + " tasks)");
            return chapterTasks.get(chapterId);
        }

        ResourceManager manager = server.getResourceManager();
        List<Task> tasks = ServerTaskLoader.loadTasks(manager, chapterId);
        chapterTasks.put(chapterId, tasks);

        // ДОБАВИТЬ ОТЛАДОЧНЫЙ ВЫВОД ДЛЯ ИНДЕКСАЦИИ
        System.out.println("[ArsMelima] Indexing " + tasks.size() + " tasks for chapter: " + chapterId);
        for (Task t : tasks) {
            String itemKey = t.getItemId().toLowerCase(Locale.ROOT);
            itemIndex.computeIfAbsent(itemKey, k -> new ArrayList<>()).add(t);
            System.out.println("[ArsMelima]   Indexed task '" + t.getId() + "' for item: " + itemKey);
        }

        return tasks;
    }
    public static synchronized Set<String> getLoadedChapterIds() {
        return new HashSet<>(chapterTasks.keySet());
    }

    public static synchronized List<Task> getAllTasks() {
        List<Task> allTasks = new ArrayList<>();
        for (List<Task> tasks : chapterTasks.values()) {
            allTasks.addAll(tasks);
        }
        return allTasks;
    }
    public static synchronized List<Task> getTasksByItem(String itemId) {
        if (itemId == null) return Collections.emptyList();
        String itemKey = itemId.toLowerCase(Locale.ROOT);
        List<Task> tasks = itemIndex.getOrDefault(itemKey, Collections.emptyList());
        System.out.println("[ArsMelima] Found " + tasks.size() + " tasks for item: " + itemKey);
        System.out.println("[ArsMelima] Available items in index: " + itemIndex.keySet());
        return tasks;
    }
    public static synchronized void registerChapterTasks(String chapterId, List<Task> tasks) {
        if (chapterId == null || tasks == null) return;
        chapterTasks.put(chapterId, tasks);
        for (Task t : tasks) {
            itemIndex.computeIfAbsent(t.getItemId().toLowerCase(java.util.Locale.ROOT), k -> new ArrayList<>()).add(t);
        }
    }

    // -----------------------
    // Compatibility helpers
    // -----------------------

    /**
     * Совместимая версия, используемая в старом коде: TaskManager.getTasksForPlayer(serverPlayer)
     * Возвращает уникальный список всех задач, известных серверу (объединённый индекс).
     * Подойдёт для обхода/поиска задач в обработчиках событий.
     */
    public static synchronized List<Task> getTasksForPlayer(ServerPlayer player) {
        if (player == null) return Collections.emptyList();
        // Собираем все уникальные задачи из itemIndex
        Set<Task> set = new LinkedHashSet<>();
        for (List<Task> lst : itemIndex.values()) {
            set.addAll(lst);
        }
        return new ArrayList<>(set);
    }
}
