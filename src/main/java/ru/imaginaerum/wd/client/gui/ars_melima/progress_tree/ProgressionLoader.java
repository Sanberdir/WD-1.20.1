package ru.imaginaerum.wd.client.gui.ars_melima.progress_tree;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.*;

public class ProgressionLoader {
    private static final String PROGRESSION_DIR = "ars_melima/progression";
    private static final Gson GSON = new Gson();

    public static List<ProgressNode> loadNodes() {
        List<ProgressNode> nodes = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> langs = getLanguageCandidates();

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? PROGRESSION_DIR : "lang/" + lang + "/" + PROGRESSION_DIR;
            try {
                Map<ResourceLocation, Resource> found = manager.listResources(basePath, rl -> rl.getPath().endsWith(".json"));
                if (found == null || found.isEmpty()) continue;

                for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
                    ResourceLocation rl = entry.getKey();
                    try (var is = entry.getValue().open(); var reader = new InputStreamReader(is)) {
                        JsonElement je = JsonParser.parseReader(reader);

                        if (je.isJsonArray()) {
                            for (JsonElement el : je.getAsJsonArray()) {
                                JsonObject jo = el.getAsJsonObject();
                                String id = jo.has("id") ? jo.get("id").getAsString() : "";

                                // --- fallback: если id пустой — взять basename файла (имя json без расширения) ---
                                if (id == null || id.isEmpty()) {
                                    String path = rl.getPath(); // e.g. "ars_melima/progression/some_id.json" or "lang/ru_ru/ars_melima/progression/some_id.json"
                                    int slash = path.lastIndexOf('/');
                                    int dot = path.lastIndexOf('.');
                                    if (dot > slash && dot >= 0) {
                                        id = path.substring(slash + 1, dot);
                                    }
                                }

                                String item = jo.has("item") ? jo.get("item").getAsString() : "";
                                String desc = jo.has("description") ? jo.get("description").getAsString() : "";
                                String parentId = jo.has("parentId") ? jo.get("parentId").getAsString() : "";
                                String side = jo.has("side") ? jo.get("side").getAsString() : "";
                                boolean locked = jo.has("locked") && jo.get("locked").getAsBoolean();

                                nodes.add(new ProgressNode(id, desc, item, parentId, side, locked));
                            }
                        } else if (je.isJsonObject()) {
                            JsonObject jo = je.getAsJsonObject();
                            String id = jo.has("id") ? jo.get("id").getAsString() : "";

                            // --- fallback: если id пустой — взять basename файла ---
                            if (id == null || id.isEmpty()) {
                                String path = rl.getPath();
                                int slash = path.lastIndexOf('/');
                                int dot = path.lastIndexOf('.');
                                if (dot > slash && dot >= 0) {
                                    id = path.substring(slash + 1, dot);
                                }
                            }

                            String item = jo.has("item") ? jo.get("item").getAsString() : "";
                            String desc = jo.has("description") ? jo.get("description").getAsString() : "";
                            String parentId = jo.has("parentId") ? jo.get("parentId").getAsString() : "";
                            String side = jo.has("side") ? jo.get("side").getAsString() : "";
                            boolean locked = jo.has("locked") && jo.get("locked").getAsBoolean();

                            nodes.add(new ProgressNode(id, desc, item, parentId, side, locked));
                        } else {
                            System.err.println("[ArsMelima] JSON in " + rl + " is neither object nor array!");
                        }
                    } catch (Exception e) {
                        System.err.println("[ArsMelima] Failed to load progression node " + rl + " : " + e.getMessage());
                    }
                }

                if (!nodes.isEmpty()) break; // используем первую найденную локализацию
            } catch (Exception e) {
                System.err.println("[ArsMelima] Error loading progression from " + basePath + " : " + e.getMessage());
            }
        }

        return nodes;
    }

    // --- дубликат логики локалей ---
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
