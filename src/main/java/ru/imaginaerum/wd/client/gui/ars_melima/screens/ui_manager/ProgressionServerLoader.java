package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;

import java.io.InputStreamReader;
import java.util.*;

public class ProgressionServerLoader {

    public static List<ProgressNode> loadNodes(MinecraftServer server) {
        List<ProgressNode> nodes = new ArrayList<>();
        if (server == null) return nodes;

        try {
            ResourceManager manager = server.getResourceManager();
            Map<ResourceLocation, Resource> found = manager.listResources("", rl ->
                    rl.getPath().toLowerCase(Locale.ROOT).contains("ars_melima/progression") && rl.getPath().endsWith(".json")
            );
            if (found == null || found.isEmpty()) return nodes;

            for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
                ResourceLocation rl = entry.getKey();

                try (var is = entry.getValue().open(); var reader = new InputStreamReader(is)) {

                    JsonElement je = JsonParser.parseReader(reader);

                    if (je.isJsonArray()) {
                        for (JsonElement el : je.getAsJsonArray()) {
                            if (!el.isJsonObject()) continue;
                            loadNodeFromJson(el.getAsJsonObject(), rl, nodes);
                        }
                    } else if (je.isJsonObject()) {
                        loadNodeFromJson(je.getAsJsonObject(), rl, nodes);
                    }

                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        return nodes;
    }


    private static void loadNodeFromJson(JsonObject jo, ResourceLocation rl, List<ProgressNode> nodes) {
        String id = jo.has("id") ? jo.get("id").getAsString() : basenameFromPath(rl.getPath());
        String item = jo.has("item") ? jo.get("item").getAsString() : "";
        String desc = jo.has("description") ? jo.get("description").getAsString() : "";
        String parent = jo.has("parentId") ? jo.get("parentId").getAsString() : "";
        String side = jo.has("side") ? jo.get("side").getAsString() : "";
        boolean locked = jo.has("locked") && jo.get("locked").getAsBoolean();

        // Новое поле: позиция корневой ноды
        int rootPos = jo.has("rootPosition") ? jo.get("rootPosition").getAsInt() : 1;

        // ← ДОБАВЛЕНО: загрузка уровня (по умолчанию 1)
        int level = jo.has("level") ? jo.get("level").getAsInt() : 1;

        // ← ОБНОВЛЕНО: добавлен параметр level
        nodes.add(new ProgressNode(id, desc, item, parent, side, locked, rootPos, level));
    }

    private static String basenameFromPath(String path) {
        if (path == null) return "";
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        if (dot > slash && dot >= 0) return path.substring(slash + 1, dot);
        return path;
    }
}
