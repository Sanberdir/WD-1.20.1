package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ServerTaskLoader {
    private static final String TASKS_DIR = "ars_melima/learning_tasks";

    /**
     * Попытка загрузить tasks для указанного learningChapterId используя переданный ResourceManager.
     * Повторяет логику client-side TaskLoader, но без обращения к Minecraft.getInstance().
     */
    public static List<Task> loadTasks(ResourceManager manager, String learningChapterId) {
        List<Task> tasks = new ArrayList<>();
        if (learningChapterId == null || learningChapterId.isEmpty()) return tasks;

        // Серверный набор языков — минимальный, можно расширить при необходимости
        String[] langs = new String[] { "en_us", "ru_ru", "__BASE__" };

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? TASKS_DIR : ("lang/" + lang + "/" + TASKS_DIR);
            String filePath = basePath + "/" + learningChapterId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            try {
                Resource res = manager.getResource(rl).orElse(null);
                if (res == null) continue;

                try (InputStream is = res.open(); InputStreamReader reader = new InputStreamReader(is)) {
                    JsonElement rootEl = JsonParser.parseReader(reader);
                    JsonArray array = null;

                    if (rootEl.isJsonArray()) {
                        array = rootEl.getAsJsonArray();
                    } else if (rootEl.isJsonObject()) {
                        JsonObject obj = rootEl.getAsJsonObject();
                        if (obj.has("tasks") && obj.get("tasks").isJsonArray()) {
                            // если есть learning_chapter — проверим соответствие
                            if (obj.has("learning_chapter")) {
                                String declared = obj.get("learning_chapter").getAsString();
                                if (!matchesChapterId(declared, learningChapterId)) {
                                    continue;
                                }
                            }
                            array = obj.getAsJsonArray("tasks");
                        } else if (looksLikeTaskObject(obj)) {
                            array = new JsonArray();
                            array.add(obj);
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }

                    for (JsonElement element : array) {
                        if (!element.isJsonObject()) continue;
                        JsonObject t = element.getAsJsonObject();

                        String id = t.has("id") ? t.get("id").getAsString() : "";
                        String itemId = t.has("item") ? t.get("item").getAsString() :
                                (t.has("item_id") ? t.get("item_id").getAsString() : "");
                        int count = t.has("count") ? t.get("count").getAsInt() :
                                (t.has("required") ? t.get("required").getAsInt() : 0);
                        String recipeType = t.has("recipe_type") ? t.get("recipe_type").getAsString() :
                                (t.has("recipeType") ? t.get("recipeType").getAsString() : "crafting");

                        if (!id.isEmpty() && !itemId.isEmpty() && count > 0) {
                            tasks.add(new Task(id, itemId, count, recipeType));
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[ArsMelima] ServerTaskLoader parse error for " + rl + " : " + e.getMessage());
                }

                if (!tasks.isEmpty()) break;

            } catch (Exception e) {
                System.out.println("[ArsMelima] ServerTaskLoader resource error " + rl + " : " + e.getMessage());
            }
        }

        return tasks;
    }

    private static boolean matchesChapterId(String declared, String requested) {
        if (declared == null || requested == null) return false;
        if (declared.equalsIgnoreCase(requested)) return true;
        String d = declared.contains(":") ? declared.substring(declared.indexOf(':') + 1) : declared;
        String r = requested.contains(":") ? requested.substring(requested.indexOf(':') + 1) : requested;
        return d.equalsIgnoreCase(r);
    }

    private static boolean looksLikeTaskObject(JsonObject obj) {
        return (obj.has("id") && (obj.has("item") || obj.has("item_id")));
    }
}
