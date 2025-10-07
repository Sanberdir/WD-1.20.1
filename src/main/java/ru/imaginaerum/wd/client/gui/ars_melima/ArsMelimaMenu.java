package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArsMelimaMenu {
    public static final int PROGRESSION_INDEX = -2; // новый "индекс" для режима дерева прогресса

    private final List<Chapter> chapters = new ArrayList<>();
    private final List<ProgressNode> progressNodes = new ArrayList<>();
    private int currentIndex = -1; // -1 = список глав

    // кэш для быстрого поиска главы по нормализованному id / title
    private final Map<String, Integer> normalizedChapterIndex = new HashMap<>();

    // НОВОЕ: мапа progression-id -> индекс главы (строится по содержимому главы)
    private final Map<String, Integer> progressionIdIndex = new HashMap<>();

    public ArsMelimaMenu() { }

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

    private Chapter dynamicChapter = null;

    /**
     * Открывает динамическую главу (не добавляя её в основной список chapters)
     * @param id идентификатор главы
     * @param elements контент главы
     */
    public void openDynamicChapter(String id, List<ChapterElement> elements) {
        if (id == null || elements == null || elements.isEmpty()) return;

        // Создаём временную главу с флагом open=true
        dynamicChapter = new Chapter(id, id, elements, true);

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

    /**
     * Закрывает динамическую главу
     */
    public void closeDynamicChapter() {
        dynamicChapter = null;
        this.currentIndex = -1;
    }
    public void closeChapter() { this.currentIndex = -1; }


    // --- progression helpers ---
    public void openProgression() { this.currentIndex = PROGRESSION_INDEX; }
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
