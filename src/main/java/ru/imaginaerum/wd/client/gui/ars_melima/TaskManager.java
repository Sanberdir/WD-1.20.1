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

    public static List<Task> getTasksByItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) return Collections.emptyList();

        String itemKey = itemId.toLowerCase(Locale.ROOT);
        List<Task> result = itemIndex.getOrDefault(itemKey, new ArrayList<>());

        System.out.println("[ArsMelima] TaskManager.getTasksByItem: '" + itemKey + "' -> " + result.size() + " tasks");

        // Если не нашли в индексе, попробуем загрузить все главы и переиндексировать
        if (result.isEmpty()) {
            System.out.println("[ArsMelima] No tasks found in index, checking all chapters...");
            for (String chapterId : getLoadedChapterIds()) {
                List<Task> chapterTasksList = chapterTasks.get(chapterId);
                for (Task task : chapterTasksList) {
                    if (task.getItemId().equalsIgnoreCase(itemKey)) {
                        result.add(task);
                        System.out.println("[ArsMelima]   Found task in chapter " + chapterId + ": " + task.getId());
                    }
                }
            }
        }

        return result;
    }

    /**
     * Получить задачи по предмету с принудительной загрузкой всех глав
     */
    public static List<Task> getTasksByItemWithForceLoad(MinecraftServer server, String itemId) {
        if (server == null || itemId == null) return Collections.emptyList();

        String itemKey = itemId.toLowerCase(Locale.ROOT);
        List<Task> result = new ArrayList<>();

        // Список всех возможных глав (добавьте сюда все ваши главы)
        String[] allChapters = {"cutting_techniques", "cooking_methods", "advanced_cooking", "magical_cooking"};

        System.out.println("[ArsMelima] Force loading tasks for item: " + itemKey);

        for (String chapterId : allChapters) {
            try {
                List<Task> tasks = getTasksForChapter(server, chapterId);
                for (Task task : tasks) {
                    if (task.getItemId().equalsIgnoreCase(itemKey)) {
                        result.add(task);
                        System.out.println("[ArsMelima]   Found in chapter " + chapterId + ": " + task.getId());
                    }
                }
            } catch (Exception e) {
                System.out.println("[ArsMelima] Error loading chapter " + chapterId + ": " + e.getMessage());
            }
        }

        System.out.println("[ArsMelima] Force load result: " + result.size() + " tasks for " + itemKey);
        return result;
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

    /**
     * Предварительная загрузка всех глав
     */
    public static synchronized void preloadAllChapters(MinecraftServer server) {
        if (server == null) return;

        String[] allChapters = {"cutting_techniques", "cooking_methods", "advanced_cooking", "magical_cooking"};

        System.out.println("[ArsMelima] Preloading all chapters...");

        for (String chapterId : allChapters) {
            try {
                getTasksForChapter(server, chapterId);
                System.out.println("[ArsMelima] ✓ Preloaded chapter: " + chapterId);
            } catch (Exception e) {
                System.out.println("[ArsMelima] ✗ Failed to preload chapter " + chapterId + ": " + e.getMessage());
            }
        }

        System.out.println("[ArsMelima] Preload complete. Loaded chapters: " + getLoadedChapterIds());
    }
}