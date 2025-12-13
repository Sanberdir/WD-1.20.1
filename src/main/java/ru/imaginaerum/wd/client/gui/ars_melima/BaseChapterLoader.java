package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.*;

public class BaseChapterLoader {
    private static final String BASE_CHAPTERS_META_DIR = "ars_melima/base_chapters"; // папка для метаданных базовых глав
    private static final String BASE_CHAPTERS_CONTENT_DIR = "ars_melima/base_content"; // папка для содержимого базовых глав

    public static List<Chapter> loadBaseChapters() {
        List<ChapterMetadata> metadataList = loadBaseChaptersMetadata();
        List<Chapter> chapters = new ArrayList<>();

        for (ChapterMetadata meta : metadataList) {
            List<ChapterElement> elements = loadBaseChapterContent(meta.getId());
            chapters.add(new Chapter(meta.getId(), meta.getTitle(), elements, meta.isOpen(), meta.getIcon()));
        }

        return chapters;
    }

    private static List<ChapterMetadata> loadBaseChaptersMetadata() {
        List<ChapterMetadata> metadataList = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> langs = getLanguageCandidates();

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? BASE_CHAPTERS_META_DIR : "lang/" + lang + "/" + BASE_CHAPTERS_META_DIR;

            try {
                Map<ResourceLocation, Resource> found = manager.listResources(basePath, rl -> rl.getPath().endsWith(".json"));
                if (found == null || found.isEmpty()) continue;

                for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
                    ResourceLocation rl = entry.getKey();

                    try (InputStream is = entry.getValue().open(); InputStreamReader reader = new InputStreamReader(is)) {
                        JsonObject jo = JsonParser.parseReader(reader).getAsJsonObject();

                        String path = rl.getPath();
                        String id = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));

                        String title = jo.has("title") ? jo.get("title").getAsString() : id;
                        boolean open = jo.has("status") && "open".equals(jo.get("status").getAsString());
                        String icon = jo.has("icon") ? jo.get("icon").getAsString() : "";

                        metadataList.add(new ChapterMetadata(id, title, open, icon));
                    } catch (Exception e) {
                        System.err.println("[ArsMelima] Failed to load base chapter metadata " + rl + " : " + e.getMessage());
                    }
                }

                if (!metadataList.isEmpty()) break;

            } catch (Exception e) {
                System.err.println("[ArsMelima] Error loading base chapter metadata from " + basePath + " : " + e.getMessage());
            }
        }

        return metadataList;
    }

    public static List<ChapterElement> loadBaseChapterContent(String chapterId) {
        List<ChapterElement> elements = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> langs = getLanguageCandidates();

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? BASE_CHAPTERS_CONTENT_DIR : "lang/" + lang + "/" + BASE_CHAPTERS_CONTENT_DIR;
            String filePath = basePath + "/" + chapterId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            try {
                Resource resource = manager.getResource(rl).orElse(null);
                if (resource == null) continue;

                try (InputStream is = resource.open(); InputStreamReader reader = new InputStreamReader(is)) {
                    JsonObject jo = JsonParser.parseReader(reader).getAsJsonObject();

                    if (jo.has("elements") && jo.get("elements").isJsonArray()) {
                        var arr = jo.getAsJsonArray("elements");
                        for (var el : arr) {
                            if (!el.isJsonObject()) continue;
                            JsonObject obj = el.getAsJsonObject();

                            if (obj.has("text")) {
                                elements.add(new ChapterElement(ChapterElement.Type.TEXT, obj.get("text").getAsString()));
                            } else if (obj.has("image")) {
                                elements.add(new ChapterElement(ChapterElement.Type.IMAGE, obj.get("image").getAsString()));
                            } else if (obj.has("type") && obj.has("data")) {
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
                    System.err.println("[ArsMelima] Failed to load base chapter content " + rl + " : " + e.getMessage());
                }

                if (!elements.isEmpty()) break;

            } catch (Exception e) {
                // Файл не найден — пробуем следующую локализацию
            }
        }

        return elements;
    }

    private static List<String> getLanguageCandidates() {
        // Та же реализация, что и в ChapterLoader
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