package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets; // Импорт для StandardCharsets
import java.nio.charset.Charset; // Для отладки
import java.util.*;

public class ChapterLoader {
    private static final String CHAPTERS_META_DIR = "ars_melima/chapters"; // папка для метаданных глав
    private static final String CHAPTERS_CONTENT_DIR = "ars_melima/content"; // папка для содержимого глав

    public static List<Chapter> loadChapters() {
        // Отладочный вывод для проверки кодировки
        System.out.println("[ArsMelima DEBUG] Default Charset: " + Charset.defaultCharset());
        System.out.println("[ArsMelima DEBUG] File encoding: " + System.getProperty("file.encoding"));

        List<ChapterMetadata> metadataList = loadChaptersMetadata();
        List<Chapter> chapters = new ArrayList<>();

        for (ChapterMetadata meta : metadataList) {
            List<ChapterElement> elements = loadChapterContent(meta.getId());
            chapters.add(new Chapter(meta.getId(), meta.getTitle(), elements, meta.isOpen(), meta.getIcon()));
        }

        return chapters;
    }

    private static List<ChapterMetadata> loadChaptersMetadata() {
        List<ChapterMetadata> metadataList = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> langs = getLanguageCandidates();

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? CHAPTERS_META_DIR : "lang/" + lang + "/" + CHAPTERS_META_DIR;

            try {
                Map<ResourceLocation, Resource> found = manager.listResources(basePath, rl -> rl.getPath().endsWith(".json"));
                if (found == null || found.isEmpty()) continue;

                for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
                    ResourceLocation rl = entry.getKey();

                    // ИСПРАВЛЕНО: добавлено StandardCharsets.UTF_8
                    try (InputStream is = entry.getValue().open();
                         InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        JsonObject jo = JsonParser.parseReader(reader).getAsJsonObject();

                        // Извлекаем ID из имени файла
                        String path = rl.getPath();
                        String id = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));

                        String title = jo.has("title") ? jo.get("title").getAsString() : id;
                        boolean open = jo.has("status") && "open".equals(jo.get("status").getAsString());
                        String icon = jo.has("icon") ? jo.get("icon").getAsString() : "";

                        // Отладочный вывод для проверки
                        if (lang.equals("ru_ru") || lang.equals("__BASE__")) {
                            System.out.println("[ArsMelima DEBUG] Loaded chapter: " + id + ", title: " + title);
                        }

                        metadataList.add(new ChapterMetadata(id, title, open, icon));
                    } catch (Exception e) {
                        System.err.println("[ArsMelima] Failed to load chapter metadata " + rl + " : " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                if (!metadataList.isEmpty()) break; // используем первую найденную локализацию

            } catch (Exception e) {
                System.err.println("[ArsMelima] Error loading chapter metadata from " + basePath + " : " + e.getMessage());
                e.printStackTrace();
            }
        }

        return metadataList;
    }

    public static List<ChapterElement> loadChapterContent(String chapterId) {
        List<ChapterElement> elements = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> langs = getLanguageCandidates();

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? CHAPTERS_CONTENT_DIR : "lang/" + lang + "/" + CHAPTERS_CONTENT_DIR;
            String filePath = basePath + "/" + chapterId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            try {
                Resource resource = manager.getResource(rl).orElse(null);
                if (resource == null) continue;

                // ИСПРАВЛЕНО: добавлено StandardCharsets.UTF_8
                try (InputStream is = resource.open();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonObject jo = JsonParser.parseReader(reader).getAsJsonObject();

                    if (jo.has("elements") && jo.get("elements").isJsonArray()) {
                        var arr = jo.getAsJsonArray("elements");
                        for (var el : arr) {
                            if (!el.isJsonObject()) continue;
                            JsonObject obj = el.getAsJsonObject();

                            // Новый формат (text/image)
                            if (obj.has("text")) {
                                String text = obj.get("text").getAsString();
                                elements.add(new ChapterElement(ChapterElement.Type.TEXT, text));

                                // Отладочный вывод для первых нескольких символов
                                if (elements.size() <= 3 && text.length() > 0) {
                                    System.out.println("[ArsMelima DEBUG] Text element loaded, first 20 chars: " +
                                            text.substring(0, Math.min(20, text.length())));
                                }
                            } else if (obj.has("image")) {
                                elements.add(new ChapterElement(ChapterElement.Type.IMAGE, obj.get("image").getAsString()));
                            }
                            // Старый формат (type/data) для совместимости
                            else if (obj.has("type") && obj.has("data")) {
                                try {
                                    ChapterElement.Type type = ChapterElement.Type.valueOf(
                                            obj.get("type").getAsString().toUpperCase(Locale.ROOT)
                                    );
                                    elements.add(new ChapterElement(type, obj.get("data").getAsString()));
                                } catch (IllegalArgumentException ignored) {}
                            }
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[ArsMelima] Failed to load chapter content " + rl + " : " + e.getMessage());
                    e.printStackTrace();
                }

                if (!elements.isEmpty()) {
                    System.out.println("[ArsMelima DEBUG] Chapter " + chapterId + " loaded successfully from lang: " + lang);
                    break; // нашли содержимое, прекращаем поиск по локалям
                }

            } catch (Exception e) {
                // Файл не найден — пробуем следующую локализацию
                System.out.println("[ArsMelima DEBUG] File not found: " + rl + ", trying next language...");
            }
        }

        if (elements.isEmpty()) {
            System.out.println("[ArsMelima DEBUG] No content found for chapter: " + chapterId);
        }

        return elements;
    }


    // getLanguageCandidates() и normalizeLangCode() остаются без изменений
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

        System.out.println("[ArsMelima DEBUG] Language candidates: " + langs);
        return new ArrayList<>(new LinkedHashSet<>(langs));
    }

    private static String normalizeLangCode(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}