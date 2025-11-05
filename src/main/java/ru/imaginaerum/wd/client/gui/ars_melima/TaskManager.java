package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.*;

public class TaskManager {
    // chapterId -> tasks
    private static final Map<String, List<Task>> chapterTasks = new HashMap<>();
    // itemId -> list of tasks (reverse index)
    private static final Map<String, List<Task>> itemIndex = new HashMap<>();

    public static synchronized List<Task> getTasksForChapter(MinecraftServer server, String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) return Collections.emptyList();

        System.out.println("[ArsMelima] TaskManager.getTasksForChapter: " + chapterId);
        System.out.println("[ArsMelima] Cached chapters: " + chapterTasks.keySet());

        if (chapterTasks.containsKey(chapterId)) {
            List<Task> cachedTasks = chapterTasks.get(chapterId);
            System.out.println("[ArsMelima] Returning CACHED tasks for chapter: " + chapterId + " (" + cachedTasks.size() + " tasks)");

            // Детальная информация о кэшированных задачах
            for (Task task : cachedTasks) {
                System.out.println("[ArsMelima] Cached task: " + task.getId() + " types: " + task.getRecipeTypes());
            }
            return cachedTasks;
        }

        System.out.println("[ArsMelima] No cache found, loading tasks for chapter: " + chapterId);
        ResourceManager manager = server.getResourceManager();
        List<Task> tasks = ServerTaskLoader.loadTasks(manager, chapterId);

        // ОЧИСТКА СТАРОГО КЭША перед обновлением
        clearChapterCache(chapterId);

        chapterTasks.put(chapterId, tasks);

        // Обновляем индекс
        System.out.println("[ArsMelima] Indexing " + tasks.size() + " tasks for chapter: " + chapterId);
        for (Task t : tasks) {
            String itemKey = t.getItemId().toLowerCase(Locale.ROOT);
            itemIndex.computeIfAbsent(itemKey, k -> new ArrayList<>()).add(t);
            System.out.println("[ArsMelima]   Indexed task '" + t.getId() + "' for item: " + itemKey + " types: " + t.getRecipeTypes());
        }

        return tasks;
    }

    public static synchronized List<Task> getTasksByItem(String itemId) {
        if (itemId == null) {
            System.out.println("[ArsMelima] WARNING: getTasksByItem called with null itemId");
            return Collections.emptyList();
        }

        String itemKey = itemId.toLowerCase(Locale.ROOT);
        List<Task> tasks = itemIndex.getOrDefault(itemKey, Collections.emptyList());

        System.out.println("[ArsMelima] TaskManager.getTasksByItem '" + itemKey + "' -> " + tasks.size() + " tasks");
        System.out.println("[ArsMelima] Available items in index: " + itemIndex.keySet());

        for (Task task : tasks) {
            System.out.println("[ArsMelima] Found task: " + task.getId() + " types: " + task.getRecipeTypes());
        }

        return tasks;
    }

    /**
     * Очистка кэша для конкретной главы
     */
    public static synchronized void clearChapterCache(String chapterId) {
        if (chapterId == null) return;

        System.out.println("[ArsMelima] Clearing cache for chapter: " + chapterId);

        // Удаляем из chapterTasks
        List<Task> removedTasks = chapterTasks.remove(chapterId);
        if (removedTasks != null) {
            System.out.println("[ArsMelima] Removed " + removedTasks.size() + " tasks from chapter cache");
        }

        // Перестраиваем itemIndex
        itemIndex.clear();
        for (Map.Entry<String, List<Task>> entry : chapterTasks.entrySet()) {
            for (Task task : entry.getValue()) {
                String itemKey = task.getItemId().toLowerCase(Locale.ROOT);
                itemIndex.computeIfAbsent(itemKey, k -> new ArrayList<>()).add(task);
            }
        }

        System.out.println("[ArsMelima] Cache cleared. Now " + chapterTasks.size() + " chapters and " + itemIndex.size() + " items in index");
    }

    public static synchronized Set<String> getLoadedChapterIds() {
        return new HashSet<>(chapterTasks.keySet());
    }


}