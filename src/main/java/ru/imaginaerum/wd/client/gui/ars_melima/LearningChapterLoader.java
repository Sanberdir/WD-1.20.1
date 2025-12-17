package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets; // Добавлен импорт
import java.nio.charset.Charset; // Для отладки
import java.util.*;

public class LearningChapterLoader {
    private static final Gson GSON = new Gson();
    private static final String LEARNING_CHAPTERS_DIR = "ars_melima/learning_chapters";

    public static List<LearningChapter> loadLearningChapters(String chapterId) {
        System.out.println("[ArsMelima Learning DEBUG] Loading learning chapters for: " + chapterId);
        System.out.println("[ArsMelima Learning DEBUG] Default Charset: " + Charset.defaultCharset());
        System.out.println("[ArsMelima Learning DEBUG] File encoding: " + System.getProperty("file.encoding"));

        List<LearningChapter> learningChapters = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> langs = getLanguageCandidates();

        System.out.println("[ArsMelima Learning DEBUG] Language candidates: " + langs);

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? LEARNING_CHAPTERS_DIR : "lang/" + lang + "/" + LEARNING_CHAPTERS_DIR;
            String filePath = basePath + "/" + chapterId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            System.out.println("[ArsMelima Learning DEBUG] Trying to load from: " + rl + " (lang: " + lang + ")");

            try {
                Resource resource = manager.getResource(rl).orElse(null);
                if (resource == null) {
                    System.out.println("[ArsMelima Learning DEBUG] Resource not found: " + rl);
                    continue;
                }

                System.out.println("[ArsMelima Learning DEBUG] Resource found, parsing...");

                // ИСПРАВЛЕНО: добавлено StandardCharsets.UTF_8
                try (InputStream is = resource.open();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonElement rootElement = JsonParser.parseReader(reader);

                    if (!rootElement.isJsonArray()) {
                        System.out.println("[ArsMelima Learning DEBUG] Root is not a JSON array");
                        continue;
                    }

                    JsonArray array = rootElement.getAsJsonArray();
                    System.out.println("[ArsMelima Learning DEBUG] Found JSON array with " + array.size() + " elements");

                    int parsedCount = 0;
                    for (JsonElement element : array) {
                        if (!element.isJsonObject()) {
                            System.out.println("[ArsMelima Learning DEBUG] Element is not a JSON object, skipping");
                            continue;
                        }

                        JsonObject obj = element.getAsJsonObject();

                        String id = obj.has("id") ? obj.get("id").getAsString() : "";
                        String title = obj.has("title") ? obj.get("title").getAsString() : "";
                        String status = obj.has("status") ? obj.get("status").getAsString() : "locked";
                        String parent = obj.has("parent") ? obj.get("parent").getAsString() : "";

                        System.out.println("[ArsMelima Learning DEBUG] Parsing learning chapter - " +
                                "ID: " + id + ", Title: " + title +
                                ", Status: " + status + ", Parent: " + parent);

                        if (!id.isEmpty()) {
                            learningChapters.add(new LearningChapter(id, title, status, parent));
                            parsedCount++;

                            // Отладочный вывод для проверки русского текста
                            if (!title.isEmpty() && !lang.equals("en_us")) {
                                System.out.println("[ArsMelima Learning DEBUG] Title loaded (lang: " + lang + "): " + title);
                                // Проверка первых символов в hex
                                if (title.length() > 0) {
                                    byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
                                    System.out.print("[ArsMelima Learning DEBUG] Title bytes (hex, first 10): ");
                                    for (int i = 0; i < Math.min(10, titleBytes.length); i++) {
                                        System.out.printf("%02X ", titleBytes[i]);
                                    }
                                    System.out.println();
                                }
                            }
                        } else {
                            System.out.println("[ArsMelima Learning DEBUG] Empty ID, skipping");
                        }
                    }

                    System.out.println("[ArsMelima Learning DEBUG] Successfully parsed " + parsedCount +
                            " learning chapters from lang: " + lang);

                } catch (Exception e) {
                    System.err.println("[ArsMelima] Failed to load learning chapters " + rl + " : " + e.getMessage());
                    e.printStackTrace();
                }

                if (!learningChapters.isEmpty()) {
                    System.out.println("[ArsMelima Learning DEBUG] Learning chapters loaded successfully, breaking language loop");
                    break; // используем первую найденную локализацию
                }

            } catch (Exception e) {
                // Файл не найден - пробуем следующую локализацию
                System.out.println("[ArsMelima Learning DEBUG] File not found or error: " + rl +
                        ", trying next language...");
            }
        }

        System.out.println("[ArsMelima Learning DEBUG] Total learning chapters loaded: " + learningChapters.size());
        return learningChapters;
    }

    // Используем тот же метод getLanguageCandidates() что и в ChapterLoader
    public static List<String> getLanguageCandidates() {
        List<String> langs = new ArrayList<>();
        try {
            Object sel = null;
            try {
                sel = Minecraft.getInstance().getLanguageManager().getSelected();
            } catch (Throwable ignored) {}

            if (sel != null) {
                if (sel instanceof String code && !code.isEmpty()) {
                    langs.add(normalizeLangCode(code));
                    System.out.println("[ArsMelima Learning DEBUG] Selected language (String): " + code);
                } else {
                    try {
                        Method m = sel.getClass().getMethod("getCode");
                        Object codeObj = m.invoke(sel);
                        if (codeObj instanceof String code && !code.isEmpty()) {
                            langs.add(normalizeLangCode(code));
                            System.out.println("[ArsMelima Learning DEBUG] Selected language (Method): " + code);
                        }
                    } catch (Throwable ignored) {}
                }
            }

            Locale locale = Locale.getDefault();
            if (locale != null) {
                langs.add(normalizeLangCode(locale.toString()));
                langs.add(normalizeLangCode(locale.getLanguage()));
                langs.add(normalizeLangCode(locale.getLanguage() + "_" + locale.getCountry()));
                System.out.println("[ArsMelima Learning DEBUG] System locale: " + locale);
            }
        } finally {
            langs.add("en_us");
            langs.add("ru_ru");
            langs.add("__BASE__");
        }

        List<String> uniqueLangs = new ArrayList<>(new LinkedHashSet<>(langs));
        System.out.println("[ArsMelima Learning DEBUG] Final language candidates: " + uniqueLangs);
        return uniqueLangs;
    }

    private static String normalizeLangCode(String raw) {
        if (raw == null) return "";
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        System.out.println("[ArsMelima Learning DEBUG] Normalized language code: '" + raw + "' -> '" + normalized + "'");
        return normalized;
    }
}