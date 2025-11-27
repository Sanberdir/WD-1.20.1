package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.tree_progress;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.util.*;

import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;

public class ProgressTreeLoader {
    private static final Gson GSON = new Gson();
    private static final String PROGRESSION_DIR = "ars_melima/progression";

    public static List<ProgressNode> loadProgressTree(String treeId) {
        if (treeId == null || treeId.isEmpty()) return Collections.emptyList();

        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> langs = getLanguageCandidates();

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? PROGRESSION_DIR : "lang/" + lang + "/" + PROGRESSION_DIR;
            String filePath = basePath + "/" + treeId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            try {
                Resource resource = manager.getResource(rl).orElse(null);
                if (resource == null) {
                    continue;
                }

                try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                    JsonElement root = JsonParser.parseReader(reader);
                    List<ProgressNode> nodes = parseProgressTree(root, treeId);

                    // Сохраняем название файла для локализации
                    ProgressTreeTitlesCache.setTitle(treeId, treeId);

                    System.out.println("[ArsMelima] Successfully loaded progress tree: " + treeId + " from " + rl + " with " + nodes.size() + " nodes");
                    return nodes;
                }
            } catch (Exception e) {
                System.err.println("[ArsMelima] Failed to load progress tree " + rl + " : " + e.getMessage());
            }
        }

        System.err.println("[ArsMelima] Progress tree file not found for id: " + treeId + " (checked language candidates)");
        return Collections.emptyList();
    }

    private static List<ProgressNode> parseProgressTree(JsonElement root, String treeId) {
        List<ProgressNode> nodes = new ArrayList<>();
        String treeTitle = null;

        if (root == null) return nodes;

        try {
            if (root.isJsonObject()) {
                JsonObject treeObj = root.getAsJsonObject();

                // Извлекаем заголовок дерева
                if (treeObj.has("title")) {
                    treeTitle = treeObj.get("title").getAsString();
                }

                if (treeObj.has("nodes") && treeObj.get("nodes").isJsonArray()) {
                    JsonArray nodesArray = treeObj.getAsJsonArray("nodes");
                    for (JsonElement nodeElem : nodesArray) {
                        ProgressNode node = parseProgressNode(nodeElem, treeId);
                        if (node != null) {
                            // Устанавливаем заголовок дерева в ноду (или в отдельную структуру)
                            nodes.add(node);
                        }
                    }
                } else {
                    ProgressNode node = parseProgressNode(root, treeId);
                    if (node != null) nodes.add(node);
                }

                // Сохраняем заголовок дерева (можно в отдельную структуру)
                if (treeTitle != null) {
                    // Сохраняем в кэш заголовков деревьев
                    ProgressTreeTitlesCache.setTitle(treeId, treeTitle);
                }
            } else if (root.isJsonArray()) {
                // Старый формат без заголовка
                JsonArray nodesArray = root.getAsJsonArray();
                for (JsonElement nodeElem : nodesArray) {
                    ProgressNode node = parseProgressNode(nodeElem, treeId);
                    if (node != null) nodes.add(node);
                }
            }
        } catch (Exception e) {
            System.err.println("[ArsMelima] Failed to parse progress tree " + treeId + " : " + e.getMessage());
        }

        return nodes;
    }

    private static ProgressNode parseProgressNode(JsonElement nodeElem, String treeId) {
        if (!nodeElem.isJsonObject()) return null;

        try {
            JsonObject nodeObj = nodeElem.getAsJsonObject();

            String id = nodeObj.has("id") ? nodeObj.get("id").getAsString() : "";
            String title = nodeObj.has("title") ? nodeObj.get("title").getAsString() : id;
            String description = nodeObj.has("description") ? nodeObj.get("description").getAsString() : "";
            String itemResource = nodeObj.has("item") ? nodeObj.get("item").getAsString() : "";

            String parentId =
                    nodeObj.has("parent") ? nodeObj.get("parent").getAsString() :
                            nodeObj.has("parentId") ? nodeObj.get("parentId").getAsString() : "";

            String side = nodeObj.has("side") ? nodeObj.get("side").getAsString() : "right";
            boolean locked = nodeObj.has("locked") && nodeObj.get("locked").getAsBoolean();

            // Новое поле rootPosition (1–4). Если нет — значение по умолчанию = 1.
            int rootPosition = 1;
            try {
                if (nodeObj.has("rootPosition")) {
                    rootPosition = nodeObj.get("rootPosition").getAsInt();
                }
            } catch (Exception ignored) {
                // если корявое значение — пусть будет 1
            }

            return new ProgressNode(id, description, itemResource, parentId, side, locked, rootPosition);

        } catch (Exception e) {
            System.err.println("[ArsMelima] Failed to parse progress node: " + e.getMessage());
            return null;
        }
    }


    public static boolean progressTreeExists(String treeId) {
        if (treeId == null || treeId.isEmpty()) return false;

        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        List<String> langs = getLanguageCandidates();

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? PROGRESSION_DIR : "lang/" + lang + "/" + PROGRESSION_DIR;
            String filePath = basePath + "/" + treeId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            try {
                if (manager.getResource(rl).isPresent()) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
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
        } catch (Throwable ignored) {
        } finally {
            // fallback order: selected locale, system locale variants, english, russian, base
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
