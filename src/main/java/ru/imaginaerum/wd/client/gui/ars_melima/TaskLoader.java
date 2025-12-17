package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets; // Добавлен импорт
import java.nio.charset.Charset; // Для отладки
import java.util.*;

public class TaskLoader {
    private static final String TASKS_DIR = "ars_melima/learning_tasks";

    public static List<Task> loadTasks(String learningChapterId) {
        System.out.println("[ArsMelima Tasks DEBUG] Loading tasks for chapter: " + learningChapterId);
        System.out.println("[ArsMelima Tasks DEBUG] Default Charset: " + Charset.defaultCharset());
        System.out.println("[ArsMelima Tasks DEBUG] File encoding: " + System.getProperty("file.encoding"));

        List<Task> tasks = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        if (learningChapterId == null || learningChapterId.isEmpty()) {
            System.out.println("[ArsMelima Tasks DEBUG] Empty chapter ID, returning debug tasks");
            return createDebugTasks(learningChapterId);
        }

        List<String> langs = LearningChapterLoader.getLanguageCandidates();
        boolean foundAnyFile = false;
        boolean foundValidTasks = false;

        System.out.println("[ArsMelima Tasks DEBUG] Language candidates: " + langs);

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? TASKS_DIR : "lang/" + lang + "/" + TASKS_DIR;
            String filePath = basePath + "/" + learningChapterId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            System.out.println("[ArsMelima Tasks DEBUG] Trying to load from: " + rl + " (lang: " + lang + ")");

            try {
                Resource resource = manager.getResource(rl).orElse(null);
                if (resource == null) {
                    System.out.println("[ArsMelima Tasks DEBUG] Resource not found: " + rl);
                    continue;
                }

                foundAnyFile = true;
                System.out.println("[ArsMelima Tasks DEBUG] Resource found, parsing...");

                // ИСПРАВЛЕНО: добавлено StandardCharsets.UTF_8
                try (InputStream is = resource.open();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonElement rootEl = JsonParser.parseReader(reader);
                    JsonArray tasksArray = null;

                    if (rootEl.isJsonArray()) {
                        tasksArray = rootEl.getAsJsonArray();
                        System.out.println("[ArsMelima Tasks DEBUG] Root is JSON array with " + tasksArray.size() + " elements");
                    } else if (rootEl.isJsonObject()) {
                        JsonObject rootObj = rootEl.getAsJsonObject();
                        System.out.println("[ArsMelima Tasks DEBUG] Root is JSON object");

                        if (rootObj.has("learning_chapter")) {
                            String declared = rootObj.get("learning_chapter").getAsString();
                            System.out.println("[ArsMelima Tasks DEBUG] Declared chapter: " + declared +
                                    ", requested: " + learningChapterId);
                            if (!matchesChapterId(declared, learningChapterId)) {
                                System.out.println("[ArsMelima Tasks DEBUG] Chapter ID mismatch, skipping");
                                continue;
                            }
                        }

                        if (rootObj.has("tasks") && rootObj.get("tasks").isJsonArray()) {
                            tasksArray = rootObj.getAsJsonArray("tasks");
                            System.out.println("[ArsMelima Tasks DEBUG] Found tasks array with " + tasksArray.size() + " elements");
                        } else if (looksLikeTaskObject(rootObj)) {
                            tasksArray = new JsonArray();
                            tasksArray.add(rootObj);
                            System.out.println("[ArsMelima Tasks DEBUG] Single task object found");
                        } else {
                            System.out.println("[ArsMelima Tasks DEBUG] No valid task structure found");
                            continue;
                        }
                    } else {
                        System.out.println("[ArsMelima Tasks DEBUG] Invalid JSON format");
                        continue;
                    }

                    int parsedTasks = 0;

                    for (JsonElement element : tasksArray) {
                        if (!element.isJsonObject()) {
                            continue;
                        }

                        JsonObject taskObj = element.getAsJsonObject();

                        String id = taskObj.has("id") ? taskObj.get("id").getAsString() : "";
                        String itemId = taskObj.has("item") ? taskObj.get("item").getAsString() :
                                (taskObj.has("item_id") ? taskObj.get("item_id").getAsString() : "");
                        int count = taskObj.has("count") ? taskObj.get("count").getAsInt() :
                                (taskObj.has("required") ? taskObj.get("required").getAsInt() : 0);

                        System.out.println("[ArsMelima Tasks DEBUG] Parsing task - ID: " + id +
                                ", Item: " + itemId + ", Count: " + count);

                        if (id.isEmpty() || itemId.isEmpty() || count <= 0) {
                            System.out.println("[ArsMelima Tasks DEBUG] Invalid task data, skipping");
                            continue;
                        }

                        List<String> recipeTypes = parseRecipeTypes(taskObj, id);

                        if (id.equals("bread_3")) {
                            recipeTypes = Arrays.asList("crafting", "smelting", "campfire_cooking", "smoking", "stonecutting");
                            System.out.println("[ArsMelima Tasks DEBUG] Special bread_3 task, recipe types: " + recipeTypes);
                        }

                        if (recipeTypes.isEmpty()) {
                            recipeTypes.add("crafting");
                        }

                        Task task = new Task(id, itemId, count, recipeTypes, learningChapterId);
                        tasks.add(task);
                        parsedTasks++;
                        foundValidTasks = true;

                        System.out.println("[ArsMelima Tasks DEBUG] Task added: " + id +
                                ", Recipe types: " + recipeTypes);
                    }

                    System.out.println("[ArsMelima Tasks DEBUG] Successfully parsed " + parsedTasks + " tasks from lang: " + lang);

                } catch (Exception e) {
                    System.err.println("[ArsMelima] Failed to parse tasks " + rl + " : " + e.getMessage());
                    e.printStackTrace();
                }

                if (!tasks.isEmpty()) {
                    System.out.println("[ArsMelima Tasks DEBUG] Tasks loaded successfully, breaking language loop");
                    break;
                }

            } catch (Exception e) {
                System.err.println("[ArsMelima] Tasks file error for " + rl + " : " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("[ArsMelima Tasks DEBUG] After language loop - Tasks found: " + tasks.size() +
                ", Found any file: " + foundAnyFile + ", Found valid tasks: " + foundValidTasks);

        if (!foundValidTasks && tasks.isEmpty()) {
            System.out.println("[ArsMelima Tasks DEBUG] No valid tasks found, trying base folder...");
            tasks.addAll(loadFromBaseFolder(manager, learningChapterId));
        }

        if (tasks.isEmpty()) {
            System.out.println("[ArsMelima Tasks DEBUG] Still no tasks, creating debug tasks...");
            tasks.addAll(createDebugTasks(learningChapterId));
        }

        System.out.println("[ArsMelima Tasks DEBUG] Final task count for chapter " + learningChapterId + ": " + tasks.size());
        return tasks;
    }

    private static List<Task> loadFromBaseFolder(ResourceManager manager, String learningChapterId) {
        System.out.println("[ArsMelima Tasks DEBUG] Loading from base folder for chapter: " + learningChapterId);
        List<Task> tasks = new ArrayList<>();
        String baseFilePath = TASKS_DIR + "/" + learningChapterId + ".json";
        ResourceLocation baseRl = new ResourceLocation("wd", baseFilePath);

        System.out.println("[ArsMelima Tasks DEBUG] Base file path: " + baseRl);

        try {
            Resource baseResource = manager.getResource(baseRl).orElse(null);
            if (baseResource != null) {
                System.out.println("[ArsMelima Tasks DEBUG] Base resource found");

                // ИСПРАВЛЕНО: добавлено StandardCharsets.UTF_8
                try (InputStream is = baseResource.open();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonElement rootEl = JsonParser.parseReader(reader);
                    JsonArray tasksArray = null;

                    if (rootEl.isJsonArray()) {
                        tasksArray = rootEl.getAsJsonArray();
                        System.out.println("[ArsMelima Tasks DEBUG] Base: Root is array with " + tasksArray.size() + " elements");
                    } else if (rootEl.isJsonObject() && rootEl.getAsJsonObject().has("tasks")) {
                        tasksArray = rootEl.getAsJsonObject().getAsJsonArray("tasks");
                        System.out.println("[ArsMelima Tasks DEBUG] Base: Found tasks array with " + tasksArray.size() + " elements");
                    }

                    if (tasksArray != null) {
                        int parsedCount = 0;
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
                                parsedCount++;

                                System.out.println("[ArsMelima Tasks DEBUG] Base folder task added: " + id);
                            }
                        }
                        System.out.println("[ArsMelima Tasks DEBUG] Base folder: Successfully parsed " + parsedCount + " tasks");
                    } else {
                        System.out.println("[ArsMelima Tasks DEBUG] Base folder: No valid tasks array found");
                    }
                }
            } else {
                System.out.println("[ArsMelima Tasks DEBUG] Base folder: Resource not found");
            }
        } catch (Exception e) {
            System.err.println("[ArsMelima] Base tasks file error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[ArsMelima Tasks DEBUG] Base folder load completed, tasks found: " + tasks.size());
        return tasks;
    }

    private static List<String> parseRecipeTypes(JsonObject taskObj, String taskId) {
        List<String> recipeTypes = new ArrayList<>();

        try {
            if (taskObj.has("recipe_types") && taskObj.get("recipe_types").isJsonArray()) {
                JsonArray typesArray = taskObj.get("recipe_types").getAsJsonArray();
                System.out.println("[ArsMelima Tasks DEBUG] Task " + taskId + ": Found recipe_types array with " + typesArray.size() + " elements");
                for (JsonElement typeElement : typesArray) {
                    if (typeElement.isJsonPrimitive()) {
                        String type = normalizeRecipeType(typeElement.getAsString());
                        if (!type.isEmpty()) {
                            recipeTypes.add(type);
                            System.out.println("[ArsMelima Tasks DEBUG] Task " + taskId + ": Recipe type: " + type);
                        }
                    }
                }
            } else if (taskObj.has("recipes") && taskObj.get("recipes").isJsonArray()) {
                JsonArray recipesArray = taskObj.get("recipes").getAsJsonArray();
                System.out.println("[ArsMelima Tasks DEBUG] Task " + taskId + ": Found recipes array");
                for (JsonElement recipeElement : recipesArray) {
                    if (recipeElement.isJsonObject()) {
                        JsonObject recipeObj = recipeElement.getAsJsonObject();
                        if (recipeObj.has("type") && recipeObj.get("type").isJsonPrimitive()) {
                            String type = normalizeRecipeType(recipeObj.get("type").getAsString());
                            if (!type.isEmpty()) {
                                recipeTypes.add(type);
                            }
                        }
                    }
                }
            } else if (taskObj.has("recipe_type") || taskObj.has("recipeType")) {
                String recipeType = taskObj.has("recipe_type") ? taskObj.get("recipe_type").getAsString() :
                        taskObj.get("recipeType").getAsString();
                String normalizedType = normalizeRecipeType(recipeType);
                recipeTypes.add(normalizedType);
                System.out.println("[ArsMelima Tasks DEBUG] Task " + taskId + ": Single recipe type: " + normalizedType);
            } else {
                recipeTypes.add("crafting");
                System.out.println("[ArsMelima Tasks DEBUG] Task " + taskId + ": Default recipe type: crafting");
            }
        } catch (Exception e) {
            System.err.println("[ArsMelima] Error parsing recipe types for task " + taskId + ": " + e.getMessage());
            e.printStackTrace();
            recipeTypes.add("crafting");
        }

        recipeTypes = new ArrayList<>(new LinkedHashSet<>(recipeTypes));
        System.out.println("[ArsMelima Tasks DEBUG] Task " + taskId + ": Final recipe types: " + recipeTypes);
        return recipeTypes;
    }

    private static String normalizeRecipeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "";
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
        System.out.println("[ArsMelima Tasks DEBUG] Normalized recipe type: '" + type + "' -> '" + normalized + "'");
        return normalized;
    }

    private static boolean matchesChapterId(String declared, String requested) {
        if (declared == null || requested == null) return false;
        if (declared.equalsIgnoreCase(requested)) return true;
        String d = declared.contains(":") ? declared.substring(declared.indexOf(':') + 1) : declared;
        String r = requested.contains(":") ? requested.substring(requested.indexOf(':') + 1) : requested;
        boolean matches = d.equalsIgnoreCase(r);
        System.out.println("[ArsMelima Tasks DEBUG] Chapter ID match - declared: " + declared +
                ", requested: " + requested + ", matches: " + matches);
        return matches;
    }

    private static boolean looksLikeTaskObject(JsonObject obj) {
        boolean looksLike = (obj.has("id") && (obj.has("item") || obj.has("item_id")) &&
                (obj.has("count") || obj.has("required")));
        System.out.println("[ArsMelima Tasks DEBUG] Object looks like task: " + looksLike);
        return looksLike;
    }

    private static List<Task> createDebugTasks(String chapterId) {
        System.out.println("[ArsMelima Tasks DEBUG] Creating debug tasks for chapter: " + chapterId);
        List<Task> debugTasks = new ArrayList<>();
        // Возвращаем пустой список вместо дебажных задач
        return debugTasks;
    }
}