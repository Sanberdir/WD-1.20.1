package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.*;

public class LearningChapterLoader {
    private static final Gson GSON = new Gson();
    private static final String LEARNING_CHAPTERS_DIR = "ars_melima/learning_chapters";

    public static List<LearningChapter> loadLearningChapters(String chapterId) {
        List<LearningChapter> learningChapters = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> langs = getLanguageCandidates();

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? LEARNING_CHAPTERS_DIR : "lang/" + lang + "/" + LEARNING_CHAPTERS_DIR;
            String filePath = basePath + "/" + chapterId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            try {
                Resource resource = manager.getResource(rl).orElse(null);
                if (resource == null) continue;

                try (InputStream is = resource.open(); InputStreamReader reader = new InputStreamReader(is)) {
                    JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();

                    for (JsonElement element : array) {
                        JsonObject obj = element.getAsJsonObject();

                        String id = obj.has("id") ? obj.get("id").getAsString() : "";
                        String title = obj.has("title") ? obj.get("title").getAsString() : "";
                        String status = obj.has("status") ? obj.get("status").getAsString() : "locked";
                        String parent = obj.has("parent") ? obj.get("parent").getAsString() : "";

                        if (!id.isEmpty()) {
                            learningChapters.add(new LearningChapter(id, title, status, parent));
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[ArsMelima] Failed to load learning chapters " + rl + " : " + e.getMessage());
                }

                if (!learningChapters.isEmpty()) break; // используем первую найденную локализацию

            } catch (Exception e) {
                // Файл не найден - пробуем следующую локализацию
            }
        }

        return learningChapters;
    }

    // Используем тот же метод getLanguageCandidates() что и в ChapterLoader
    private static List<String> getLanguageCandidates() {
        List<String> langs = new ArrayList<>();
        try {
            Object sel = null;
            try { sel = Minecraft.getInstance().getLanguageManager().getSelected(); } catch (Throwable ignored) {}
            if (sel != null) {
                if (sel instanceof String code && !code.isEmpty()) langs.add(normalizeLangCode(code));
                else {
                    try {
                        Method m = sel.getClass().getMethod("getCode");
                        Object codeObj = m.invoke(sel);
                        if (codeObj instanceof String code && !code.isEmpty()) langs.add(normalizeLangCode(code));
                    } catch (Throwable ignored) {}
                }
            }
            Locale locale = Locale.getDefault();
            if (locale != null) {
                langs.add(normalizeLangCode(locale.toString()));
                langs.add(normalizeLangCode(locale.getLanguage()));
                langs.add(normalizeLangCode(locale.getLanguage() + "_" + locale.getCountry()));
            }
        } finally {
            langs.add("en_us");
            langs.add("ru_ru");
            langs.add("__BASE__");
        }
        return new ArrayList<>(new LinkedHashSet<>(langs));
    }

    private static String normalizeLangCode(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}