package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.tree_progress.ProgressTreeLoader;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.tree_progress.ProgressTreeTitlesCache;

import java.util.*;

public class ArsMelimaMenu {
    public static final int PROGRESSION_INDEX = -2;
    public static final int LEARNING_CHAPTERS_INDEX = -3;
    public static final int TASKS_INDEX = -4;

    private final List<Chapter> chapters = new ArrayList<>();
    private final List<ProgressNode> progressNodes = new ArrayList<>();
    private int currentIndex = -1;
    private final Map<String, String> progressTreeTitles = new HashMap<>();

    private final Map<String, Integer> normalizedChapterIndex = new HashMap<>();
    private final Map<String, Integer> progressionIdIndex = new HashMap<>();

    private final Map<String, List<LearningChapter>> learningChaptersCache = new HashMap<>();
    private String currentLearningChapterId = null;

    private final Map<String, List<Task>> tasksCache = new HashMap<>();
    private String currentTaskChapterId = null;
    private int currentPage = 0;

    private final Map<String, List<TreeLink>> treeLinksCache = new HashMap<>();
    private Chapter dynamicChapter = null;

    // Новые поля для управления деревьями прогрессии
    private String currentProgressTreeId = null;
    private final Map<String, List<ProgressNode>> progressTreesCache = new HashMap<>();

    public ArsMelimaMenu() { }

    // --- Page refresh ---
    public void refreshPage() { updatePage(); }
    public void updatePage() { System.out.println("[ArsMelima] Page updated: currentPage=" + currentPage); }

    // --- Tasks ---
    public void openTasks(String learningChapterId) {
        this.currentTaskChapterId = learningChapterId;
        this.currentIndex = TASKS_INDEX;
    }
    public void closeTasks() {
        this.currentTaskChapterId = null;
        this.currentIndex = LEARNING_CHAPTERS_INDEX;
    }
    public boolean isChapterOpen() {
        return currentIndex >= 0 && currentIndex < chapters.size();
    }
    public boolean isTasksOpen() { return this.currentIndex == TASKS_INDEX; }
    public String getCurrentTaskChapterId() { return currentTaskChapterId; }
    public List<Task> getCurrentTasks() { return currentTaskChapterId != null ? getTasks(currentTaskChapterId) : Collections.emptyList(); }
    public List<Task> getTasks(String learningChapterId) {
        return tasksCache.computeIfAbsent(learningChapterId, id -> {
            List<Task> loaded = TaskLoader.loadTasks(id);
            if (loaded == null) return Collections.emptyList();
            System.out.println("[ArsMelima] Loaded " + loaded.size() + " tasks for chapter: " + id);
            return loaded;
        });
    }

    // --- Learning chapters ---
    public List<LearningChapter> getLearningChapters(String chapterId) {
        return learningChaptersCache.computeIfAbsent(chapterId, LearningChapterLoader::loadLearningChapters);
    }
    public void openLearningChapters(String chapterId) {
        this.currentLearningChapterId = chapterId;
        this.currentIndex = LEARNING_CHAPTERS_INDEX;
        refreshLearningChaptersStatus();
    }
    public void closeLearningChapters() {
        this.currentLearningChapterId = null;
        this.currentIndex = -1;
    }
    public boolean isLearningChaptersOpen() { return this.currentIndex == LEARNING_CHAPTERS_INDEX; }
    public String getCurrentLearningChapterId() { return currentLearningChapterId; }
    public List<LearningChapter> getCurrentLearningChapters() {
        if (currentLearningChapterId == null) return Collections.emptyList();
        List<LearningChapter> chapters = getLearningChapters(currentLearningChapterId);
        refreshLearningChaptersStatus(chapters);
        return chapters;
    }
    private void refreshLearningChaptersStatus() {
        if (currentLearningChapterId != null) {
            List<LearningChapter> chapters = learningChaptersCache.get(currentLearningChapterId);
            if (chapters != null) refreshLearningChaptersStatus(chapters);
        }
    }
    private void refreshLearningChaptersStatus(List<LearningChapter> learningChapters) {
        if (learningChapters == null) return;
        boolean changed = false;
        for (LearningChapter lc : learningChapters) {
            if (lc != null && lc.isLocked()) {
                String parentId = lc.getParent();
                if (parentId != null && isLearningChapterCompleted(parentId)) {
                    lc.unlock();
                    changed = true;
                    System.out.println("[ArsMelima] Auto-unlocked chapter: " + lc.getId());
                }
            }
        }
        if (changed) markLearningChaptersDirty();
    }
    private boolean isLearningChapterCompleted(String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) return false;
        List<Task> tasks = TaskLoader.loadTasks(chapterId);
        if (tasks == null || tasks.isEmpty()) return true;
        for (Task t : tasks) {
            if (ClientTaskData.getTaskProgress(chapterId, t.getId()) < t.getRequiredCount()) return false;
        }
        return true;
    }

    // --- Chapters ---
    public void setChapters(List<Chapter> list) {
        chapters.clear();
        normalizedChapterIndex.clear();
        progressionIdIndex.clear();
        if (list != null) chapters.addAll(list);

        for (int i = 0; i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            if (c == null) continue;

            String rawId = c.getId();
            if (rawId != null && !rawId.isEmpty()) {
                String normFull = normalizeKey(rawId);
                normalizedChapterIndex.putIfAbsent(normFull, i);
                int colon = rawId.indexOf(':');
                if (colon >= 0 && colon + 1 < rawId.length()) {
                    normalizedChapterIndex.putIfAbsent(normalizeKey(rawId.substring(colon + 1)), i);
                }
            }

            String title = c.getTitle();
            if (title != null && !title.isEmpty()) normalizedChapterIndex.putIfAbsent(normalizeKey(title), i);

            List<ChapterElement> elems = c.getElements();
            if (elems != null) {
                for (ChapterElement el : elems) {
                    if (el == null) continue;
                    String data = el.getData() != null ? el.getData().toString() : null;
                    if (data == null || data.isEmpty()) continue;
                    try {
                        JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
                        if (obj.has("id")) {
                            String pid = normalizeKey(obj.get("id").getAsString());
                            progressionIdIndex.putIfAbsent(pid, i);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        System.out.println("[ArsMelima] setChapters() loaded " + chapters.size() + " chapters.");
    }

    public int getChapterIndexByNormalizedKey(String key) {
        return normalizedChapterIndex.getOrDefault(normalizeKey(key), -1);
    }

    public int getChapterIndexByProgressionId(String progressionId) {
        return progressionIdIndex.getOrDefault(normalizeKey(progressionId), getChapterIndexByNormalizedKey(progressionId));
    }

    public List<Chapter> getChapters() { return chapters; }

    public void setProgressNodes(List<ProgressNode> nodes) {
        progressNodes.clear();
        if (nodes != null) progressNodes.addAll(nodes);
    }

    public List<ProgressNode> getProgressNodes() { return progressNodes; }

    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int idx) { this.currentIndex = idx; }

    public void openChapter(int idx) {
        if (idx >= 0 && idx < chapters.size()) {
            this.currentIndex = idx;
            this.currentProgressTreeId = null; // Сбрасываем ID дерева при открытии главы
        }
    }

    public Chapter getCurrentChapter() {
        if (currentIndex >= 0 && currentIndex < chapters.size()) {
            return chapters.get(currentIndex);
        } else if (dynamicChapter != null && currentIndex >= chapters.size()) {
            return dynamicChapter; // Динамические главы
        }
        return null;
    }
    public void closeChapter() {
        this.currentIndex = -1;
        this.currentProgressTreeId = null;
    }
    public void openDynamicChapter(String id, List<ChapterElement> elements) {
        dynamicChapter = new Chapter(id, id, elements, true, null);
        currentIndex = chapters.size(); // Уникальный индекс для динамических глав
        this.currentProgressTreeId = null;
        refreshPage();
    }

    public boolean isDynamicChapterOpen() {
        return dynamicChapter != null && currentIndex >= chapters.size();
    }

    // --- TreeLinks ---
    public List<TreeLink> getTreeLinks(String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) return Collections.emptyList();
        return treeLinksCache.computeIfAbsent(chapterId, TreeLinkLoader::loadTreeLinks);
    }

    public List<TreeLink> getCurrentTreeLinks() {
        Chapter ch = getCurrentChapter();
        if (ch == null) return Collections.emptyList();
        return getTreeLinks(ch.getId());
    }

    public void unlockLearningChapter(String id) {
        if (id == null || id.isEmpty()) return;
        boolean changed = false;
        for (List<LearningChapter> list : learningChaptersCache.values()) {
            if (list == null) continue;
            for (LearningChapter lc : list) {
                if (lc != null && id.equals(lc.getId()) && lc.isLocked()) {
                    lc.unlock();
                    changed = true;
                    System.out.println("[ArsMelima] Learning chapter unlocked: " + id);
                }
            }
        }
        if (changed) markLearningChaptersDirty();
    }

    private void markLearningChaptersDirty() {
        System.out.println("[ArsMelima] markLearningChaptersDirty() — cache marked for refresh.");
    }

    // --- Progression Trees ---

    /**
     * Открывает дерево прогрессии по умолчанию (все узлы)
     */
    public void openProgression() {
        this.currentIndex = PROGRESSION_INDEX;
        this.currentProgressTreeId = "default";
        // Используем уже загруженные узлы
        System.out.println("[ArsMelima] Opened default progression with " + progressNodes.size() + " nodes");
    }

    /**
     * Открывает конкретное дерево прогрессии по ID
     */
    public void openProgression(String treeId) {
        this.currentIndex = PROGRESSION_INDEX;
        this.currentProgressTreeId = treeId;
        loadProgressTree(treeId);
    }

    /**
     * Загружает дерево прогрессии по ID
     */
    public Component getProgressTreeTitle(String treeId) {
        if (treeId == null || treeId.isEmpty()) {
            return Component.literal("Progression Tree");
        }

        // Создаем ключ локализации на основе имени файла
        String localizationKey = "wd.progression." + treeId + ".title";
        return Component.translatable(localizationKey);
    }

    // В методе loadProgressTree добавляем:
    private void loadProgressTree(String treeId) {
        if (treeId == null || treeId.isEmpty()) {
            System.err.println("[ArsMelima] Cannot load progress tree: treeId is null or empty");
            return;
        }

        List<ProgressNode> nodes = progressTreesCache.computeIfAbsent(treeId, ProgressTreeLoader::loadProgressTree);

        if (nodes != null && !nodes.isEmpty()) {
            setProgressNodes(nodes);
            System.out.println("[ArsMelima] Loaded progress tree: " + treeId + " with " + nodes.size() + " nodes");
        } else {
            System.err.println("[ArsMelima] Failed to load progress tree: " + treeId);
            System.out.println("[ArsMelima] Using existing progress nodes: " + progressNodes.size() + " nodes");
        }
    }


    /**
     * Проверяет, существует ли дерево прогрессии с указанным ID
     */
    public boolean progressTreeExists(String treeId) {
        if (treeId == null || treeId.isEmpty()) return false;

        // Проверяем кэш
        if (progressTreesCache.containsKey(treeId)) {
            List<ProgressNode> nodes = progressTreesCache.get(treeId);
            return nodes != null && !nodes.isEmpty();
        }

        // Проверяем существование файла
        return ProgressTreeLoader.progressTreeExists(treeId);
    }

    /**
     * Возвращает ID текущего открытого дерева прогрессии
     */
    public String getCurrentProgressTreeId() {
        return currentProgressTreeId;
    }

    public void closeProgression() {
        this.currentIndex = -1;
        this.currentProgressTreeId = null;
    }

    public boolean isProgressionOpen() {
        return this.currentIndex == PROGRESSION_INDEX;
    }

    private static String normalizeKey(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.contains(":")) s = s.substring(s.indexOf(':') + 1);
        s = s.replace('-', '_').replace(' ', '_');
        s = s.replaceAll("[^a-z0-9_]", "");
        return s;
    }
}