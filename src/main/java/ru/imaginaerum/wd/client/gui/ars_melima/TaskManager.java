package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.*;

/**
 * TaskManager — подстраивается под структуру:
 *  - assets/wd/lang/<lang>/ars_melima/learning_tasks/*.json
 *  - assets/wd/ars_melima/learning_tasks/*.json  (BASE)
 *
 * Ведёт подробный лог для отладки обнаружения/загрузки глав и перестраивает индекс.
 */
public class TaskManager {
    private static final Map<String, List<Task>> chapterTasks = new HashMap<>();
    private static final Map<String, List<Task>> itemIndex = new HashMap<>();
    private static final String TASKS_DIR = "ars_melima/learning_tasks";

    public static synchronized List<Task> getTasksForChapter(MinecraftServer server, String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) return Collections.emptyList();
        if (chapterTasks.containsKey(chapterId)) return chapterTasks.get(chapterId);

        System.out.println("[ArsMelima] Loading chapter: " + chapterId);
        ResourceManager manager = server.getResourceManager();

        // Используем ServerTaskLoader, он уже реализует логику по языкам и BASE
        List<Task> tasks = ServerTaskLoader.loadTasks(manager, chapterId);
        System.out.println("[ArsMelima] Loaded " + tasks.size() + " tasks for chapter: " + chapterId);

        // Обновляем кэш/индекс
        clearChapterCache(chapterId);
        chapterTasks.put(chapterId, tasks);

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

        // Если не нашли в индексе — сканируем уже загруженные главы
        if (result.isEmpty()) {
            System.out.println("[ArsMelima] Item '" + itemId + "' not found in index, scanning loaded chapters...");
            for (String chapterId : getLoadedChapterIds()) {
                List<Task> chapterTasksList = chapterTasks.get(chapterId);
                if (chapterTasksList == null) continue;
                for (Task task : chapterTasksList) {
                    if (task.getItemId().equalsIgnoreCase(itemKey)) result.add(task);
                }
            }
        }

        return result;
    }

    /**
     * Принудительная загрузка всех глав и поиск задач по itemId
     */
    public static List<Task> getTasksByItemWithForceLoad(MinecraftServer server, String itemId) {
        if (server == null || itemId == null) return Collections.emptyList();
        String itemKey = itemId.toLowerCase(Locale.ROOT);
        List<Task> result = new ArrayList<>();

        System.out.println("[ArsMelima] Force-loading all chapters for item: " + itemId);
        List<String> allChapters = discoverChapterIds(server.getResourceManager());

        for (String chapterId : allChapters) {
            try {
                List<Task> tasks = getTasksForChapter(server, chapterId);
                for (Task task : tasks) {
                    if (task.getItemId().equalsIgnoreCase(itemKey)) result.add(task);
                }
            } catch (Exception e) {
                System.err.println("[ArsMelima] Failed to load chapter '" + chapterId + "' while searching for item '" + itemKey + "': " + e.getMessage());
            }
        }

        System.out.println("[ArsMelima] Found " + result.size() + " tasks for item: " + itemId);
        return result;
    }

    public static synchronized void clearChapterCache(String chapterId) {
        if (chapterId == null) return;
        List<Task> removedTasks = chapterTasks.remove(chapterId);
        System.out.println("[ArsMelima] Cleared cache for chapter: " + chapterId + ", removed tasks: " + (removedTasks != null ? removedTasks.size() : 0));

        // перестроение индекса
        itemIndex.clear();
        for (Map.Entry<String, List<Task>> entry : chapterTasks.entrySet()) {
            for (Task task : entry.getValue()) {
                String itemKey = task.getItemId().toLowerCase(Locale.ROOT);
                itemIndex.computeIfAbsent(itemKey, k -> new ArrayList<>()).add(task);
            }
        }
        System.out.println("[ArsMelima] Rebuilt item index, total items indexed: " + itemIndex.size());
    }

    public static synchronized Set<String> getLoadedChapterIds() {
        return new HashSet<>(chapterTasks.keySet());
    }

    public static synchronized void preloadAllChapters(MinecraftServer server) {
        if (server == null) return;
        System.out.println("[ArsMelima] Preloading all chapters...");
        List<String> chapterIds = discoverChapterIds(server.getResourceManager());
        for (String chapterId : chapterIds) {
            try {
                getTasksForChapter(server, chapterId);
            } catch (Exception e) {
                System.err.println("[ArsMelima] Failed to preload chapter: " + chapterId);
            }
        }
        System.out.println("[ArsMelima] Preload complete.");
    }

    /**
     * Обнаружение глав: пытаемся найти *.json в папке задач с учётом языков (как в TaskLoader).
     * Если не найдём ничего — возвращаем fallback-лист.
     */
    public static List<String> discoverChapterIds(ResourceManager resourceManager) {
        List<String> chapterIds = new ArrayList<>();
        List<String> langs = getLanguageCandidates(); // собираем все языки

        // Список известных глав — можно добавить новые здесь
        List<String> possibleChapters = Arrays.asList(
                "1st_level",
                "2st_level",
                "3st_level",
                "4st_level",
                "5st_level"
        );

        for (String chapterId : possibleChapters) {
            boolean found = false;
            for (String lang : langs) {
                String basePath = "__BASE__".equals(lang) ? TASKS_DIR : "lang/" + lang + "/" + TASKS_DIR;
                ResourceLocation rl = new ResourceLocation("wd", basePath + "/" + chapterId + ".json");
                try {
                    Resource r = resourceManager.getResource(rl).orElse(null);
                    if (r != null) {
                        chapterIds.add(chapterId);
                        System.out.println("[ArsMelima] Found chapter: " + chapterId + " at " + rl);
                        found = true;
                        break; // нашли главу — больше не ищем по другим языкам
                    }
                } catch (Exception e) {
                    // ничего не делаем, пробуем следующий язык
                }
            }
            if (!found) {
                System.out.println("[ArsMelima] Chapter not found in any language: " + chapterId);
            }
        }

        System.out.println("[ArsMelima] Total chapters discovered: " + chapterIds.size() + " -> " + chapterIds);
        return chapterIds;
    }


    private static List<String> getLanguageCandidates() {
        List<String> langs = new ArrayList<>();
        try {
            Object sel = Minecraft.getInstance().getLanguageManager().getSelected();
            if (sel instanceof String code && !code.isEmpty()) langs.add(normalizeLangCode(code));
        } catch (Throwable ignored) {}
        Locale locale = Locale.getDefault();
        if (locale != null) {
            langs.add(normalizeLangCode(locale.toString()));
            langs.add(normalizeLangCode(locale.getLanguage()));
            langs.add(normalizeLangCode(locale.getLanguage() + "_" + locale.getCountry()));
        }
        langs.add("en_us");
        langs.add("ru_ru");
        langs.add("__BASE__");
        return new ArrayList<>(new LinkedHashSet<>(langs));
    }

    private static String normalizeLangCode(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
