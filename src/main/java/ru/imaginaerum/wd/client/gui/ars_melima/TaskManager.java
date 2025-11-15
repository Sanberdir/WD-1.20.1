package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
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
            return chapterTasks.get(chapterId);
        }

        ResourceManager manager = server.getResourceManager();
        List<Task> tasks = ServerTaskLoader.loadTasks(manager, chapterId);

        // ОЧИСТКА СТАРОГО КЭША перед обновлением
        clearChapterCache(chapterId);

        chapterTasks.put(chapterId, tasks);

        // Обновляем индекс
        for (Task t : tasks) {
            String itemKey = t.getItemId().toLowerCase(Locale.ROOT);
            itemIndex.computeIfAbsent(itemKey, k -> new ArrayList<>()).add(t);
        }

        return tasks;
    }

    public static List<Task> getTasksByItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) return Collections.emptyList();

        String itemKey = itemId.toLowerCase(Locale.ROOT);
        List<Task> result = itemIndex.getOrDefault(itemKey, new ArrayList<>());

        // Если не нашли в индексе, попробуем загрузить все главы и переиндексировать
        if (result.isEmpty()) {
            for (String chapterId : getLoadedChapterIds()) {
                List<Task> chapterTasksList = chapterTasks.get(chapterId);
                for (Task task : chapterTasksList) {
                    if (task.getItemId().equalsIgnoreCase(itemKey)) {
                        result.add(task);
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

        for (String chapterId : allChapters) {
            try {
                List<Task> tasks = getTasksForChapter(server, chapterId);
                for (Task task : tasks) {
                    if (task.getItemId().equalsIgnoreCase(itemKey)) {
                        result.add(task);
                    }
                }
            } catch (Exception e) {
                // Игнорируем ошибки загрузки глав
            }
        }

        return result;
    }

    /**
     * Очистка кэша для конкретной главы
     */
    public static synchronized void clearChapterCache(String chapterId) {
        if (chapterId == null) return;

        // Удаляем из chapterTasks
        List<Task> removedTasks = chapterTasks.remove(chapterId);

        // Перестраиваем itemIndex
        itemIndex.clear();
        for (Map.Entry<String, List<Task>> entry : chapterTasks.entrySet()) {
            for (Task task : entry.getValue()) {
                String itemKey = task.getItemId().toLowerCase(Locale.ROOT);
                itemIndex.computeIfAbsent(itemKey, k -> new ArrayList<>()).add(task);
            }
        }
    }

    public static synchronized Set<String> getLoadedChapterIds() {
        return new HashSet<>(chapterTasks.keySet());
    }

    /**
     * Предварительная загрузка всех глав
     */
    public static synchronized void preloadAllChapters(MinecraftServer server) {
        if (server == null) return;

        // Динамически находим все JSON файлы в папке задач
        List<String> chapterIds = discoverChapterIds(server.getResourceManager());

        for (String chapterId : chapterIds) {
            try {
                getTasksForChapter(server, chapterId);
            } catch (Exception e) {
                System.err.println("[ArsMelima] Failed to preload chapter: " + chapterId);
            }
        }
    }
    private static final String TASKS_DIR = "ars_melima/learning_tasks";

    private static List<String> discoverChapterIds(ResourceManager resourceManager) {
        List<String> chapterIds = new ArrayList<>();
        String basePath = TASKS_DIR;

        try {
            // Ищем все файлы в папке задач
            Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                    basePath,
                    location -> location.getPath().endsWith(".json")
            );

            for (ResourceLocation rl : resources.keySet()) {
                String filename = rl.getPath();
                // Извлекаем chapterId из имени файла
                if (filename.startsWith(basePath + "/") && filename.endsWith(".json")) {
                    String chapterId = filename.substring(
                            basePath.length() + 1,
                            filename.length() - 5
                    );
                    chapterIds.add(chapterId);
                    System.out.println("[ArsMelima] Discovered chapter: " + chapterId);
                }
            }
        } catch (Exception e) {
            System.err.println("[ArsMelima] Error discovering chapters: " + e.getMessage());
        }

        // Fallback на старый список, если не нашли файлов
        if (chapterIds.isEmpty()) {
            chapterIds.addAll(Arrays.asList(
                    "cutting_techniques", "cooking_methods", "advanced_cooking", "magical_cooking"
            ));
        }

        return chapterIds;
    }
}