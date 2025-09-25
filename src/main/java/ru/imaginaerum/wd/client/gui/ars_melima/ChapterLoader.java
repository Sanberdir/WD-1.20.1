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

public class ChapterLoader {
    private static final Gson GSON = new Gson();
    private static final String BASE = "lang";
    private static final String SUBDIR = "ars_melima";

    public static List<Chapter> loadChapters() {
        List<Chapter> out = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> langs = getLanguageCandidates();
        Set<String> seen = new HashSet<>();

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? "lang/" + SUBDIR : "lang/" + lang + "/" + SUBDIR;

            try {
                Map<ResourceLocation, Resource> found = manager.listResources(basePath, rl -> rl.getPath().endsWith(".json"));
                if (found == null || found.isEmpty()) continue;

                for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
                    ResourceLocation rl = entry.getKey();
                    String uniqueKey = rl.toString();
                    if (seen.contains(uniqueKey)) continue;
                    seen.add(uniqueKey);

                    Resource resource = entry.getValue();
                    try (InputStream is = resource.open(); InputStreamReader reader = new InputStreamReader(is)) {
                        JsonObject jo = JsonParser.parseReader(reader).getAsJsonObject();
                        String title = jo.has("title") ? jo.get("title").getAsString() : rl.getPath();
                        List<ChapterElement> elements = new ArrayList<>();

                        if (jo.has("elements") && jo.get("elements").isJsonArray()) {
                            JsonArray arr = jo.getAsJsonArray("elements");
                            for (JsonElement el : arr) {
                                JsonObject obj = el.getAsJsonObject();
                                if (!obj.has("type") || !obj.has("data")) continue;
                                ChapterElement.Type type;
                                try {
                                    type = ChapterElement.Type.valueOf(obj.get("type").getAsString().toUpperCase(Locale.ROOT));
                                } catch (IllegalArgumentException e) {
                                    continue;
                                }
                                elements.add(new ChapterElement(type, obj.get("data").getAsString()));
                            }
                        }

                        // fallback: если elements пустой, пробуем content и image как один элемент
                        if (elements.isEmpty()) {
                            if (jo.has("content")) {
                                elements.add(new ChapterElement(ChapterElement.Type.TEXT, jo.get("content").getAsString()));
                            }
                            if (jo.has("image")) {
                                elements.add(new ChapterElement(ChapterElement.Type.IMAGE, jo.get("image").getAsString()));
                            }
                        }

                        // id — имя файла
                        String path = rl.getPath();
                        String id = path.substring(path.lastIndexOf('/') + 1);
                        out.add(new Chapter(id, title, elements));
                    } catch (Exception e) {
                        System.err.println("[ArsMelima] Failed to load chapter resource " + rl + " : " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                if (!out.isEmpty()) return out;
            } catch (Exception e) {
                System.err.println("[ArsMelima] Error listing/loading chapter resources in " + basePath + " : " + e.getMessage());
            }
        }

        return out;
    }

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
