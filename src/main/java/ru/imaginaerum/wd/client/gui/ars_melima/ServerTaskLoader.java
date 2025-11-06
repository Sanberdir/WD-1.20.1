package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class ServerTaskLoader {
    private static final String TASKS_DIR = "ars_melima/learning_tasks";

    public static List<Task> loadTasks(ResourceManager manager, String learningChapterId) {
        List<Task> tasks = new ArrayList<>();
        if (learningChapterId == null || learningChapterId.isEmpty()) {
            System.out.println("[ArsMelima] ServerTaskLoader: empty learningChapterId");
            return createDebugTasks(learningChapterId);
        }

        System.out.println("[ArsMelima] === SERVER START loadTasks ===");
        System.out.println("[ArsMelima] Server loading tasks for chapter: " + learningChapterId);

        String[] langs = new String[] { "ru_ru", "en_us", "__BASE__" };

        boolean foundAnyFile = false;
        boolean foundValidTasks = false;

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? TASKS_DIR : ("lang/" + lang + "/" + TASKS_DIR);
            String filePath = basePath + "/" + learningChapterId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            System.out.println("[ArsMelima] Server checking: " + rl);

            try {
                Resource res = manager.getResource(rl).orElse(null);
                if (res == null) {
                    System.out.println("[ArsMelima] ❌ Server tasks file not found: " + rl);
                    continue;
                }

                System.out.println("[ArsMelima] ✅ Server found tasks file: " + rl);
                foundAnyFile = true;

                try (InputStream is = res.open(); InputStreamReader reader = new InputStreamReader(is)) {
                    JsonElement rootEl = JsonParser.parseReader(reader);
                    System.out.println("[ArsMelima] Server JSON root element type: " + rootEl.getClass().getSimpleName());

                    JsonArray tasksArray = null;

                    if (rootEl.isJsonArray()) {
                        tasksArray = rootEl.getAsJsonArray();
                        System.out.println("[ArsMelima] Server loading tasks from array format");
                    } else if (rootEl.isJsonObject()) {
                        JsonObject rootObj = rootEl.getAsJsonObject();
                        System.out.println("[ArsMelima] Server JSON object keys: " + rootObj.keySet());

                        if (rootObj.has("learning_chapter")) {
                            String declared = rootObj.get("learning_chapter").getAsString();
                            System.out.println("[ArsMelima] Server found learning_chapter: " + declared);
                            if (!matchesChapterId(declared, learningChapterId)) {
                                System.out.println("[ArsMelima] ❌ Server chapter mismatch: '" + declared + "' != '" + learningChapterId + "'");
                                continue;
                            }
                        }

                        if (rootObj.has("tasks") && rootObj.get("tasks").isJsonArray()) {
                            tasksArray = rootObj.getAsJsonArray("tasks");
                            System.out.println("[ArsMelima] ✅ Server loading tasks from object format");
                        } else if (looksLikeTaskObject(rootObj)) {
                            tasksArray = new JsonArray();
                            tasksArray.add(rootObj);
                            System.out.println("[ArsMelima] Server loading single task from object");
                        } else {
                            System.out.println("[ArsMelima] ❌ Server unsupported JSON structure");
                            continue;
                        }
                    } else {
                        System.out.println("[ArsMelima] ❌ Server unexpected JSON element");
                        continue;
                    }

                    int parsedTasks = 0;
                    System.out.println("[ArsMelima] Server tasks array size: " + tasksArray.size());

                    for (JsonElement element : tasksArray) {
                        if (!element.isJsonObject()) {
                            System.out.println("[ArsMelima] Server skipping non-object element");
                            continue;
                        }

                        JsonObject taskObj = element.getAsJsonObject();
                        System.out.println("[ArsMelima] Server task object keys: " + taskObj.keySet());

                        String id = taskObj.has("id") ? taskObj.get("id").getAsString() : "";
                        String itemId = taskObj.has("item") ? taskObj.get("item").getAsString() :
                                (taskObj.has("item_id") ? taskObj.get("item_id").getAsString() : "");
                        int count = taskObj.has("count") ? taskObj.get("count").getAsInt() :
                                (taskObj.has("required") ? taskObj.get("required").getAsInt() : 0);

                        System.out.println("[ArsMelima] Server parsed task data - id: '" + id + "', item: '" + itemId + "', count: " + count);

                        if (id.isEmpty() || itemId.isEmpty() || count <= 0) {
                            System.out.println("[ArsMelima] ❌ Server skipping invalid task");
                            continue;
                        }

                        if (!isValidItemId(itemId)) {
                            System.out.println("[ArsMelima] ❌ Server invalid item ID: " + itemId);
                            continue;
                        }

                        List<String> recipeTypes = parseRecipeTypes(taskObj, id);

                        // ВРЕМЕННОЕ ИСПРАВЛЕНИЕ: принудительно устанавливаем типы для bread_3
                        if (id.equals("bread_3")) {
                            System.out.println("[ArsMelima] 🎯 Server applying forced recipe types for bread_3");
                            recipeTypes = Arrays.asList("crafting", "smelting", "campfire_cooking", "smoking", "stonecutting");
                        }

                        if (recipeTypes.isEmpty()) {
                            System.out.println("[ArsMelima] ⚠️ Server no recipe types, using fallback");
                            recipeTypes.add("crafting");
                        }

                        Task task = new Task(id, itemId, count, recipeTypes, learningChapterId);
                        tasks.add(task);
                        parsedTasks++;
                        foundValidTasks = true;

                        System.out.println("[ArsMelima] ✅ Server loaded task: " + id + " - " + itemId + " x" + count + " types: " + recipeTypes);
                    }

                    System.out.println("[ArsMelima] Server successfully parsed " + parsedTasks + " tasks from " + rl);

                } catch (Exception e) {
                    System.err.println("[ArsMelima] ❌ Server failed to parse tasks " + rl + " : " + e.getMessage());
                    e.printStackTrace();
                }

                if (!tasks.isEmpty()) {
                    System.out.println("[ArsMelima] 🎯 Server successfully loaded " + tasks.size() + " tasks, stopping search");
                    break;
                }

            } catch (Exception e) {
                System.out.println("[ArsMelima] ❌ Server tasks file error for " + rl + " : " + e.getMessage());
            }
        }

        System.out.println("[ArsMelima] Server search results - foundAnyFile: " + foundAnyFile + ", foundValidTasks: " + foundValidTasks);

        if (!foundValidTasks && tasks.isEmpty()) {
            System.out.println("[ArsMelima] 🔍 Server no tasks found in localized folders, checking base folder...");
            tasks.addAll(loadFromBaseFolder(manager, learningChapterId));
        }

        if (tasks.isEmpty()) {
            System.out.println("[ArsMelima] 🛠️ Server no tasks found for " + learningChapterId + ", creating debug tasks");
            tasks.addAll(createDebugTasks(learningChapterId));
        }

        System.out.println("[ArsMelima] === SERVER FINAL RESULT ===");
        System.out.println("[ArsMelima] Server loaded " + tasks.size() + " tasks for chapter: " + learningChapterId);

        for (Task task : tasks) {
            System.out.println("[ArsMelima] 📋 Server final task: " + task.getId() +
                    " -> " + task.getItemId() + " x" + task.getRequiredCount() +
                    " types: " + task.getRecipeTypes());
        }

        System.out.println("[ArsMelima] === SERVER END loadTasks ===");

        return tasks;
    }

    private static List<Task> loadFromBaseFolder(ResourceManager manager, String learningChapterId) {
        List<Task> tasks = new ArrayList<>();
        String baseFilePath = TASKS_DIR + "/" + learningChapterId + ".json";
        ResourceLocation baseRl = new ResourceLocation("wd", baseFilePath);

        System.out.println("[ArsMelima] 🔍 Server checking base folder: " + baseRl);

        try {
            Resource baseResource = manager.getResource(baseRl).orElse(null);
            if (baseResource != null) {
                System.out.println("[ArsMelima] ✅ Server found base tasks file: " + baseRl);

                try (InputStream is = baseResource.open(); InputStreamReader reader = new InputStreamReader(is)) {
                    JsonElement rootEl = JsonParser.parseReader(reader);
                    JsonArray tasksArray = null;

                    if (rootEl.isJsonArray()) {
                        tasksArray = rootEl.getAsJsonArray();
                        System.out.println("[ArsMelima] Server base: Array format");
                    } else if (rootEl.isJsonObject() && rootEl.getAsJsonObject().has("tasks")) {
                        tasksArray = rootEl.getAsJsonObject().getAsJsonArray("tasks");
                        System.out.println("[ArsMelima] Server base: Object with tasks array");
                    }

                    if (tasksArray != null) {
                        for (JsonElement element : tasksArray) {
                            if (!element.isJsonObject()) continue;
                            JsonObject taskObj = element.getAsJsonObject();

                            String id = taskObj.has("id") ? taskObj.get("id").getAsString() : "";
                            String itemId = taskObj.has("item") ? taskObj.get("item").getAsString() :
                                    (taskObj.has("item_id") ? taskObj.get("item_id").getAsString() : "");
                            int count = taskObj.has("count") ? taskObj.get("count").getAsInt() :
                                    (taskObj.has("required") ? taskObj.get("required").getAsInt() : 0);

                            if (!id.isEmpty() && !itemId.isEmpty() && count > 0) {
                                List<String> recipeTypes = parseRecipeTypes(taskObj, id);

                                if (id.equals("bread_3")) {
                                    recipeTypes = Arrays.asList("crafting", "smelting", "campfire_cooking", "smoking", "stonecutting");
                                }

                                Task task = new Task(id, itemId, count, recipeTypes, learningChapterId);
                                tasks.add(task);
                                System.out.println("[ArsMelima] ✅ Server loaded from base: " + id + " - " + itemId + " types: " + recipeTypes);
                            }
                        }
                    }
                }
            } else {
                System.out.println("[ArsMelima] ❌ Server base tasks file not found: " + baseRl);
            }
        } catch (Exception e) {
            System.out.println("[ArsMelima] ❌ Server base tasks file error: " + e.getMessage());
        }

        return tasks;
    }

    private static List<String> parseRecipeTypes(JsonObject taskObj, String taskId) {
        List<String> recipeTypes = new ArrayList<>();

        try {
            System.out.println("[ArsMelima] 🔍 Server parsing recipe types for task: " + taskId);
            System.out.println("[ArsMelima] Server task object has recipe_types: " + taskObj.has("recipe_types"));
            System.out.println("[ArsMelima] Server task object has recipes: " + taskObj.has("recipes"));
            System.out.println("[ArsMelima] Server task object has recipe_type: " + taskObj.has("recipe_type"));

            if (taskObj.has("recipe_types") && taskObj.get("recipe_types").isJsonArray()) {
                JsonArray typesArray = taskObj.get("recipe_types").getAsJsonArray();
                System.out.println("[ArsMelima] Server found recipe_types array with " + typesArray.size() + " elements");

                for (JsonElement typeElement : typesArray) {
                    if (typeElement.isJsonPrimitive()) {
                        String type = normalizeRecipeType(typeElement.getAsString());
                        if (!type.isEmpty()) {
                            recipeTypes.add(type);
                            System.out.println("[ArsMelima]   Server added recipe type: " + type);
                        }
                    }
                }

            } else if (taskObj.has("recipes") && taskObj.get("recipes").isJsonArray()) {
                JsonArray recipesArray = taskObj.get("recipes").getAsJsonArray();
                System.out.println("[ArsMelima] Server found recipes array with " + recipesArray.size() + " elements");

                for (JsonElement recipeElement : recipesArray) {
                    if (recipeElement.isJsonObject()) {
                        JsonObject recipeObj = recipeElement.getAsJsonObject();
                        if (recipeObj.has("type") && recipeObj.get("type").isJsonPrimitive()) {
                            String type = normalizeRecipeType(recipeObj.get("type").getAsString());
                            if (!type.isEmpty()) {
                                recipeTypes.add(type);
                                System.out.println("[ArsMelima]   Server added recipe type: " + type);
                            }
                        }
                    }
                }

            } else if (taskObj.has("recipe_type") || taskObj.has("recipeType")) {
                String recipeType = taskObj.has("recipe_type") ? taskObj.get("recipe_type").getAsString() :
                        taskObj.get("recipeType").getAsString();
                String normalizedType = normalizeRecipeType(recipeType);
                recipeTypes.add(normalizedType);
                System.out.println("[ArsMelima] Server found single recipe type: " + normalizedType);
            } else {
                System.out.println("[ArsMelima] ⚠️ Server no recipe types found, using default");
                recipeTypes.add("crafting");
            }

        } catch (Exception e) {
            System.err.println("[ArsMelima] ❌ Server error parsing recipe types for task " + taskId + ": " + e.getMessage());
            recipeTypes.add("crafting");
        }

        recipeTypes = new ArrayList<>(new LinkedHashSet<>(recipeTypes));
        System.out.println("[ArsMelima] Server final recipe types for " + taskId + ": " + recipeTypes);

        return recipeTypes;
    }

    private static String normalizeRecipeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "";
        }
        return type.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
    }

    private static boolean isValidItemId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        try {
            String[] parts = itemId.split(":");
            return parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean matchesChapterId(String declared, String requested) {
        if (declared == null || requested == null) return false;
        if (declared.equalsIgnoreCase(requested)) return true;
        String d = declared.contains(":") ? declared.substring(declared.indexOf(':') + 1) : declared;
        String r = requested.contains(":") ? requested.substring(requested.indexOf(':') + 1) : requested;
        return d.equalsIgnoreCase(r);
    }

    private static boolean looksLikeTaskObject(JsonObject obj) {
        return (obj.has("id") && (obj.has("item") || obj.has("item_id")) &&
                (obj.has("count") || obj.has("required")));
    }

    private static List<Task> createDebugTasks(String chapterId) {
        List<Task> debugTasks = new ArrayList<>();

        // Добавляем задачу для golden_carrot в главу cooking_methods
        if ("cooking_methods".equals(chapterId)) {
            Task goldenCarrotTask = new Task(
                    "gold",
                    "minecraft:golden_carrot",
                    5,
                    Arrays.asList("crafting", "smelting"),
                    chapterId
            );
            debugTasks.add(goldenCarrotTask);
            System.out.println("[ArsMelima] 🛠️ Server created DEBUG task for golden_carrot in cooking_methods");
        }

        // Добавляем другие debug задачи по необходимости...

        System.out.println("[ArsMelima] 🛠️ Server created " + debugTasks.size() + " DEBUG tasks for chapter: " + chapterId);
        return debugTasks;
    }
}