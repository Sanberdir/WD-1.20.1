package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TaskLoader {
    private static final String TASKS_DIR = "ars_melima/learning_tasks";

    public static List<Task> loadTasks(String learningChapterId) {
        List<Task> tasks = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        if (learningChapterId == null || learningChapterId.isEmpty()) {
            System.out.println("[ArsMelima] loadTasks: empty learningChapterId");
            return tasks;
        }

        List<String> langs = LearningChapterLoader.getLanguageCandidates();

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? TASKS_DIR : "lang/" + lang + "/" + TASKS_DIR;
            String filePath = basePath + "/" + learningChapterId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            try {
                Resource resource = manager.getResource(rl).orElse(null);
                if (resource == null) {
                    continue;
                }

                System.out.println("[ArsMelima] Found tasks file: " + rl);

                try (InputStream is = resource.open(); InputStreamReader reader = new InputStreamReader(is)) {
                    JsonElement rootEl = JsonParser.parseReader(reader);
                    JsonArray array = null;

                    if (rootEl.isJsonArray()) {
                        // ЕСЛИ ФАЙЛ - ПРОСТО МАССИВ (ваш случай)
                        array = rootEl.getAsJsonArray();
                        System.out.println("[ArsMelima] Loading tasks from array format");
                    } else if (rootEl.isJsonObject()) {
                        JsonObject obj = rootEl.getAsJsonObject();
                        // если есть поле "tasks" - используем его
                        if (obj.has("tasks") && obj.get("tasks").isJsonArray()) {
                            // проверяем соответствие learning_chapter
                            if (obj.has("learning_chapter")) {
                                String declared = obj.get("learning_chapter").getAsString();
                                if (!matchesChapterId(declared, learningChapterId)) {
                                    System.out.println("[ArsMelima] Tasks file " + rl + " has learning_chapter='" + declared + "' which does not match requested '" + learningChapterId + "'. Skipping.");
                                    continue;
                                }
                            }
                            array = obj.getAsJsonArray("tasks");
                            System.out.println("[ArsMelima] Loading tasks from object format with learning_chapter");
                        } else if (looksLikeTaskObject(obj)) {
                            // единичная задача
                            array = new JsonArray();
                            array.add(obj);
                            System.out.println("[ArsMelima] Loading single task from object");
                        } else {
                            System.out.println("[ArsMelima] Tasks file " + rl + " has unsupported JSON structure.");
                            continue;
                        }
                    } else {
                        System.out.println("[ArsMelima] Tasks file " + rl + " has unexpected JSON element.");
                        continue;
                    }

                    // Парсим массив задач
                    for (JsonElement element : array) {
                        if (!element.isJsonObject()) continue;
                        JsonObject t = element.getAsJsonObject();

                        String id = t.has("id") ? t.get("id").getAsString() : "";
                        String itemId = t.has("item") ? t.get("item").getAsString() : (t.has("item_id") ? t.get("item_id").getAsString() : "");
                        int count = t.has("count") ? t.get("count").getAsInt() : (t.has("required") ? t.get("required").getAsInt() : 0);
                        String recipeType = t.has("recipe_type") ? t.get("recipe_type").getAsString() : (t.has("recipeType") ? t.get("recipeType").getAsString() : "crafting");

                        if (!id.isEmpty() && !itemId.isEmpty() && count > 0) {
                            tasks.add(new Task(id, itemId, count, recipeType));
                            System.out.println("[ArsMelima] Loaded task: " + id + " - " + itemId + " x" + count);
                        } else {
                            System.out.println("[ArsMelima] Skipping invalid task entry in " + rl + " : id='" + id + "' item='" + itemId + "' count=" + count);
                        }
                    }

                    System.out.println("[ArsMelima] Loaded " + tasks.size() + " tasks from " + rl);

                } catch (Exception e) {
                    System.err.println("[ArsMelima] Failed to parse tasks " + rl + " : " + e.getMessage());
                }

                if (!tasks.isEmpty()) break;

            } catch (Exception e) {
                System.out.println("[ArsMelima] Tasks file error for " + rl + " : " + e.getMessage());
            }
        }

        if (tasks.isEmpty()) {
            System.out.println("[ArsMelima] No tasks found for " + learningChapterId + ", creating debug tasks");
            tasks.addAll(createDebugTasks(learningChapterId));
        } else {
            System.out.println("[ArsMelima] Successfully loaded " + tasks.size() + " tasks for chapter: " + learningChapterId);
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

    // Простейшая эвристика: объект похоже на задачу (есть поля item/item_id и id)
    private static boolean looksLikeTaskObject(JsonObject obj) {
        return (obj.has("id") && (obj.has("item") || obj.has("item_id")));
    }

    // Метод для создания тестовых задач (можно удалить после настройки)
    private static List<Task> createDebugTasks(String chapterId) {
        List<Task> debugTasks = new ArrayList<>();
        debugTasks.add(new Task("bread_3", "minecraft:bread", 3, "crafting"));
        debugTasks.add(new Task("porkchop_6", "minecraft:cooked_porkchop", 6, "smelting"));
        return debugTasks;
    }
}
