package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;

import java.util.*;

public class ArsMelimaMenu {
    public static final int PROGRESSION_INDEX = -2; // новый "индекс" для режима дерева прогресса
    public static final int LEARNING_CHAPTERS_INDEX = -3; // новое состояние для learning chapters
    public static final int TASKS_INDEX = -4; // новое состояние для списка задач

    private final List<Chapter> chapters = new ArrayList<>();
    private final List<ProgressNode> progressNodes = new ArrayList<>();
    private int currentIndex = -1; // -1 = список глав

    // кэш для быстрого поиска главы по нормализованному id / title
    private final Map<String, Integer> normalizedChapterIndex = new HashMap<>();

    // НОВОЕ: мапа progression-id -> индекс главы (строится по содержимому главы)
    private final Map<String, Integer> progressionIdIndex = new HashMap<>();

    // Кэш для learning chapters
    private final Map<String, List<LearningChapter>> learningChaptersCache = new HashMap<>();
    private String currentLearningChapterId = null; // ID главы, для которой показываем learning chapters

    private final Map<String, List<Task>> tasksCache = new HashMap<>();
    private String currentTaskChapterId = null;
    private int currentPage = 0;

    private Chapter dynamicChapter = null;

    public ArsMelimaMenu() { }

    public void refreshPage() {
        this.currentPage = this.currentPage;
        this.updatePage();
    }

    public void updatePage() {
        System.out.println("[ArsMelima] Page updated: currentPage=" + currentPage);
    }

    public void openTasks(String learningChapterId) {
        this.currentTaskChapterId = learningChapterId;
        this.currentIndex = TASKS_INDEX;
    }

    public void closeTasks() {
        this.currentTaskChapterId = null;
        this.currentIndex = LEARNING_CHAPTERS_INDEX; // возврат к learning chapters
    }

    public boolean isTasksOpen() {
        return this.currentIndex == TASKS_INDEX;
    }

    public String getCurrentTaskChapterId() {
        return currentTaskChapterId;
    }

    public List<Task> getCurrentTasks() {
        if (currentTaskChapterId != null) {
            return getTasks(currentTaskChapterId);
        }
        return Collections.emptyList();
    }

    public List<Task> getTasks(String learningChapterId) {
        List<Task> tasks = tasksCache.computeIfAbsent(learningChapterId, id -> {
            List<Task> loadedTasks = TaskLoader.loadTasks(id);
            System.out.println("[ArsMelima] Loaded " + loadedTasks.size() + " tasks for chapter: " + id);
            for (Task task : loadedTasks) {
                System.out.println("  - " + task.getId() + ": " + task.getItemId() + " x" + task.getRequiredCount());
            }
            return loadedTasks;
        });
        return tasks;
    }

    public List<LearningChapter> getLearningChapters(String chapterId) {
        return learningChaptersCache.computeIfAbsent(chapterId,
                id -> LearningChapterLoader.loadLearningChapters(id));
    }


    // === LEARNING CHAPTERS METHODS ===
    public void openLearningChapters(String chapterId) {
        this.currentLearningChapterId = chapterId;
        this.currentIndex = LEARNING_CHAPTERS_INDEX;

        // НОВОЕ: Автоматическая проверка разблокировки при открытии
        refreshLearningChaptersStatus();
    }

    public void closeLearningChapters() {
        this.currentLearningChapterId = null;
        this.currentIndex = -1;
    }

    public boolean isLearningChaptersOpen() {
        return this.currentIndex == LEARNING_CHAPTERS_INDEX;
    }

    public String getCurrentLearningChapterId() {
        return currentLearningChapterId;
    }

    public List<LearningChapter> getCurrentLearningChapters() {
        if (currentLearningChapterId != null) {
            List<LearningChapter> chapters = getLearningChapters(currentLearningChapterId);
            // НОВОЕ: Всегда проверяем статус разблокировки при получении списка
            refreshLearningChaptersStatus(chapters);
            return chapters;
        }
        return Collections.emptyList();
    }

    // НОВЫЙ МЕТОД: Обновление статуса разблокировки для всех learning chapters
    public void refreshLearningChaptersStatus() {
        if (currentLearningChapterId != null) {
            List<LearningChapter> chapters = learningChaptersCache.get(currentLearningChapterId);
            if (chapters != null) {
                refreshLearningChaptersStatus(chapters);
            }
        }
    }

    // НОВЫЙ МЕТОД: Обновление статуса разблокировки для конкретного списка глав
    private void refreshLearningChaptersStatus(List<LearningChapter> learningChapters) {
        if (learningChapters == null) return;

        boolean changed = false;

        for (LearningChapter lc : learningChapters) {
            if (lc != null && lc.isLocked()) {
                // Проверяем, выполнены ли все задачи родительской главы
                String parentChapterId = lc.getParent();
                if (parentChapterId != null && !parentChapterId.isEmpty()) {
                    if (isLearningChapterCompleted(parentChapterId)) {
                        lc.unlock();
                        changed = true;
                        System.out.println("[ArsMelima] Auto-unlocked chapter: " + lc.getId() +
                                " (parent " + parentChapterId + " completed)");
                    }
                }
            }
        }

        if (changed) {
            markLearningChaptersDirty();
        }
    }

    // НОВЫЙ МЕТОД: Проверка выполнения всех задач главы
    private boolean isLearningChapterCompleted(String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) return false;

        List<Task> tasks = TaskLoader.loadTasks(chapterId);
        if (tasks == null || tasks.isEmpty()) return true; // Если нет задач - считаем выполненной

        for (Task t : tasks) {
            int progress = ClientTaskData.getTaskProgress(chapterId, t.getId());
            if (progress < t.getRequiredCount()) return false;
        }
        return true;
    }

    public void setChapters(List<Chapter> list) {
        chapters.clear();
        normalizedChapterIndex.clear();
        progressionIdIndex.clear();
        if (list != null) chapters.addAll(list);

        for (int i = 0; i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            if (c == null) continue;

            // --- нормализованные ключи по главе ---
            String rawId = c.getId();
            if (rawId != null && !rawId.isEmpty()) {
                String normFull = normalizeKey(rawId);
                normalizedChapterIndex.putIfAbsent(normFull, i);

                int colon = rawId.indexOf(':');
                if (colon >= 0 && colon + 1 < rawId.length()) {
                    String noNs = normalizeKey(rawId.substring(colon + 1));
                    normalizedChapterIndex.putIfAbsent(noNs, i);
                }
            }

            String title = c.getTitle();
            if (title != null && !title.isEmpty()) {
                String tnorm = normalizeKey(title);
                normalizedChapterIndex.putIfAbsent(tnorm, i);
            }

            // --- progression-id из элементов ---
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

        System.out.println("[ArsMelima] setChapters() loaded " + chapters.size() +
                " chapters. normalizedKeys=" + normalizedChapterIndex.keySet() +
                " progressionKeys=" + progressionIdIndex.keySet());
    }

    /**
     * Основной поиск главы по "ключу" (используется старым кодом).
     * Оставлен без изменений — сначала проверяет normalizedChapterIndex и т.д.
     */
    public int getChapterIndexByNormalizedKey(String key) {
        if (key == null || key.isEmpty()) return -1;
        String k = normalizeKey(key);

        // 1) быстрый прямой поиск в мапе
        Integer v = normalizedChapterIndex.get(k);
        if (v != null) return v;

        // 2) попробуем без namespace (если в ключе он есть)
        int colon = key.indexOf(':');
        if (colon >= 0 && colon + 1 < key.length()) {
            String withoutNs = normalizeKey(key.substring(colon + 1));
            v = normalizedChapterIndex.get(withoutNs);
            if (v != null) return v;
        }

        // 3) попробуем перебор по всем сохранённым ключам (на случай разных нормализаций)
        for (Map.Entry<String, Integer> e : normalizedChapterIndex.entrySet()) {
            String mapKey = e.getKey();
            if (mapKey.equals(k) || mapKey.equalsIgnoreCase(k) || mapKey.startsWith(k) || k.startsWith(mapKey)) {
                return e.getValue();
            }
        }

        // 4) дополнительный перебор по исходным данным (id/title) - более надёжно, но медленнее
        for (int i = 0; i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            if (c == null) continue;
            String rawId = c.getId() != null ? c.getId() : "";
            String title  = c.getTitle() != null ? c.getTitle() : "";

            if (rawId.equalsIgnoreCase(key) || rawId.equalsIgnoreCase(k) ||
                    title.equalsIgnoreCase(key) || title.equalsIgnoreCase(k)) {
                return i;
            }

            if (normalizeKey(rawId).equals(k) || normalizeKey(title).equals(k)) {
                return i;
            }

            int cidx = rawId.indexOf(':');
            if (cidx >= 0 && cidx + 1 < rawId.length()) {
                String noNs = normalizeKey(rawId.substring(cidx + 1));
                if (noNs.equals(k)) return i;
            }
        }

        System.out.println("[ArsMelima] getChapterIndexByNormalizedKey FAILED for key='" + key + "' normalized='" + k + "'. MapKeys=" + normalizedChapterIndex.keySet());

        return -1;
    }

    /**
     * НОВЫЙ метод: поиск главы по id из progression (сначала по построенной мапе progressionIdIndex,
     * затем fallback на обычный поиск по normalizedChapterIndex).
     */
    public int getChapterIndexByProgressionId(String progressionId) {
        if (progressionId == null || progressionId.isEmpty()) return -1;
        String k = normalizeKey(progressionId);

        Integer v = progressionIdIndex.get(k);
        if (v != null) return v;

        // попробуем без namespace
        int colon = progressionId.indexOf(':');
        if (colon >= 0 && colon + 1 < progressionId.length()) {
            String withoutNs = normalizeKey(progressionId.substring(colon + 1));
            v = progressionIdIndex.get(withoutNs);
            if (v != null) return v;
        }

        // токены из ключа
        String[] parts = k.split("_");
        for (String p : parts) {
            if (p.length() <= 1) continue;
            v = progressionIdIndex.get(p);
            if (v != null) return v;
        }

        // fallback на старый поиск по нормализованным id/title
        return getChapterIndexByNormalizedKey(progressionId);
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
        if (idx >= 0 && idx < chapters.size()) this.currentIndex = idx;
    }

    /**
     * Открывает динамическую главу (не добавляя её в основной список chapters)
     * @param id идентификатор главы
     * @param elements контент главы
     */
    public void openDynamicChapter(String id, List<ChapterElement> elements) {
        if (id == null || elements == null || elements.isEmpty()) return;

        // Создаём временную главу с флагом open=true и без иконки
        dynamicChapter = new Chapter(id, id, elements, true, null);

        // Устанавливаем currentIndex в специальное значение для динамической главы
        this.currentIndex = PROGRESSION_INDEX - 1; // любое уникальное отрицательное значение, отличное от PROGRESSION_INDEX
    }

    /**
     * Получение текущей главы с учётом динамической
     */
    public Chapter getCurrentChapter() {
        if (currentIndex >= 0 && currentIndex < chapters.size()) {
            return chapters.get(currentIndex);
        }
        if (currentIndex == PROGRESSION_INDEX - 1 && dynamicChapter != null) {
            return dynamicChapter;
        }
        return null;
    }

    public void closeChapter() { this.currentIndex = -1; }

    /**
     * Разблокирует learning chapter по ID.
     * Меняет статус на "unlocked" и очищает кэш, чтобы обновить отображение.
     */
    public void unlockLearningChapter(String id) {
        if (id == null || id.isEmpty()) return;

        boolean changed = false;

        for (Map.Entry<String, List<LearningChapter>> entry : learningChaptersCache.entrySet()) {
            List<LearningChapter> list = entry.getValue();
            if (list == null) continue;

            for (LearningChapter lc : list) {
                if (lc != null && id.equals(lc.getId()) && lc.isLocked()) {
                    lc.unlock();
                    changed = true;
                    System.out.println("[ArsMelima] Learning chapter unlocked: " + id);
                }
            }
        }

        if (changed) {
            markLearningChaptersDirty();
        }
    }

    /**
     * Заглушка для обновления кэша UI после разблокировки learning chapters.
     */
    private void markLearningChaptersDirty() {
        System.out.println("[ArsMelima] markLearningChaptersDirty() — cache marked for refresh.");
    }

    // --- progression helpers ---
    public void closeProgression() { this.currentIndex = -1; }
    public boolean isProgressionOpen() { return this.currentIndex == PROGRESSION_INDEX; }

    // Нормализация: lower, убрать namespace перед двоеточием, заменить дефисы/пробелы на '_',
    // оставить только a-z0-9_ для стабильного сравнения.
    private static String normalizeKey(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.contains(":")) {
            // сохраняем и вариант с модулем, но при ключе убираем модуль
            s = s.substring(s.indexOf(':') + 1);
        }
        s = s.replace('-', '_').replace(' ', '_');
        s = s.replaceAll("[^a-z0-9_]", "");
        return s;
    }
}