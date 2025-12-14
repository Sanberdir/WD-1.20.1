package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.util.*;

public class TreeLinkLoader {
    private static final Gson GSON = new Gson();
    private static final String TREE_LINKS_DIR = "ars_melima/tree_links";

    public static List<TreeLink> loadTreeLinks(String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) return Collections.emptyList();

        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> langs = getLanguageCandidates();

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? TREE_LINKS_DIR : "lang/" + lang + "/" + TREE_LINKS_DIR;
            String filePath = basePath + "/" + chapterId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            try {
                Resource resource = manager.getResource(rl).orElse(null);
                if (resource == null) continue;

                try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                    JsonElement root = JsonParser.parseReader(reader);
                    List<TreeLink> parsed = parseJson(root);
                    if (!parsed.isEmpty()) {
                        return parsed;
                    }
                }
            } catch (Exception e) {
                System.err.println("[ArsMelima] Failed to load TreeLinks " + rl + " : " + e.getMessage());
            }
        }

        return Collections.emptyList();
    }

    private static List<TreeLink> parseJson(JsonElement root) {
        if (root == null || root.isJsonNull()) return Collections.emptyList();
        List<TreeLink> result = new ArrayList<>();

        try {
            JsonArray arr;
            if (root.isJsonObject() && root.getAsJsonObject().has("links") && root.getAsJsonObject().get("links").isJsonArray()) {
                arr = root.getAsJsonObject().getAsJsonArray("links");
            } else if (root.isJsonArray()) {
                arr = root.getAsJsonArray();
            } else {
                return result;
            }

            for (JsonElement el : arr) {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                    String id = el.getAsString();
                    // Раньше было: result.add(new TreeLink(id, id, null));
                    // Сейчас: сохраняем title как null, чтобы рендер мог попытаться найти перевод
                    result.add(new TreeLink(id, null, null));
                } else if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    String id = obj.has("id") ? obj.get("id").getAsString() : null;
                    String title = obj.has("title") ? obj.get("title").getAsString() : id;
                    String icon = obj.has("icon") ? obj.get("icon").getAsString() : null;
                    if (id != null) result.add(new TreeLink(id, title, icon));
                }
            }
        } catch (Exception ignored) {}

        return result;
    }

    private static List<String> getLanguageCandidates() {
        List<String> langs = new ArrayList<>();
        try {
            Object sel = Minecraft.getInstance().getLanguageManager().getSelected();
            if (sel != null) {
                if (sel instanceof String code && !code.isEmpty()) langs.add(normalizeLangCode(code));
                else {
                    try {
                        var m = sel.getClass().getMethod("getCode");
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
