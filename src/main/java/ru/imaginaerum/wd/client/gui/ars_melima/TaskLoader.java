package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class TaskLoader {
    private static final String TASKS_DIR = "ars_melima/learning_tasks";

    public static List<Task> loadTasks(String learningChapterId) {
        List<Task> tasks = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        if (learningChapterId == null || learningChapterId.isEmpty()) {
            return createDebugTasks(learningChapterId);
        }

        List<String> langs = LearningChapterLoader.getLanguageCandidates();
        boolean foundAnyFile = false;
        boolean foundValidTasks = false;

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? TASKS_DIR : "lang/" + lang + "/" + TASKS_DIR;
            String filePath = basePath + "/" + learningChapterId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            try {
                Resource resource = manager.getResource(rl).orElse(null);
                if (resource == null) {
                    continue;
                }

                foundAnyFile = true;

                try (InputStream is = resource.open(); InputStreamReader reader = new InputStreamReader(is)) {
                    JsonElement rootEl = JsonParser.parseReader(reader);
                    JsonArray tasksArray = null;

                    if (rootEl.isJsonArray()) {
                        tasksArray = rootEl.getAsJsonArray();
                    } else if (rootEl.isJsonObject()) {
                        JsonObject rootObj = rootEl.getAsJsonObject();

                        if (rootObj.has("learning_chapter")) {
                            String declared = rootObj.get("learning_chapter").getAsString();
                            if (!matchesChapterId(declared, learningChapterId)) {
                                continue;
                            }
                        }

                        if (rootObj.has("tasks") && rootObj.get("tasks").isJsonArray()) {
                            tasksArray = rootObj.getAsJsonArray("tasks");
                        } else if (looksLikeTaskObject(rootObj)) {
                            tasksArray = new JsonArray();
                            tasksArray.add(rootObj);
                        } else {
                            continue;
                        }
                    } else {
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

                        if (id.isEmpty() || itemId.isEmpty() || count <= 0) {
                            continue;
                        }

                        List<String> recipeTypes = parseRecipeTypes(taskObj, id);

                        if (id.equals("bread_3")) {
                            recipeTypes = Arrays.asList("crafting", "smelting", "campfire_cooking", "smoking", "stonecutting");
                        }

                        if (recipeTypes.isEmpty()) {
                            recipeTypes.add("crafting");
                        }

                        Task task = new Task(id, itemId, count, recipeTypes, learningChapterId);
                        tasks.add(task);
                        parsedTasks++;
                        foundValidTasks = true;
                    }

                } catch (Exception e) {
                    System.err.println("[ArsMelima] Failed to parse tasks " + rl + " : " + e.getMessage());
                }

                if (!tasks.isEmpty()) {
                    break;
                }

            } catch (Exception e) {
                System.err.println("[ArsMelima] Tasks file error for " + rl + " : " + e.getMessage());
            }
        }

        if (!foundValidTasks && tasks.isEmpty()) {
            tasks.addAll(loadFromBaseFolder(manager, learningChapterId));
        }

        if (tasks.isEmpty()) {
            tasks.addAll(createDebugTasks(learningChapterId));
        }

        return tasks;
    }

    private static List<Task> loadFromBaseFolder(ResourceManager manager, String learningChapterId) {
        List<Task> tasks = new ArrayList<>();
        String baseFilePath = TASKS_DIR + "/" + learningChapterId + ".json";
        ResourceLocation baseRl = new ResourceLocation("wd", baseFilePath);

        try {
            Resource baseResource = manager.getResource(baseRl).orElse(null);
            if (baseResource != null) {
                try (InputStream is = baseResource.open(); InputStreamReader reader = new InputStreamReader(is)) {
                    JsonElement rootEl = JsonParser.parseReader(reader);
                    JsonArray tasksArray = null;

                    if (rootEl.isJsonArray()) {
                        tasksArray = rootEl.getAsJsonArray();
                    } else if (rootEl.isJsonObject() && rootEl.getAsJsonObject().has("tasks")) {
                        tasksArray = rootEl.getAsJsonObject().getAsJsonArray("tasks");
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
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ArsMelima] Base tasks file error: " + e.getMessage());
        }

        return tasks;
    }

    private static List<String> parseRecipeTypes(JsonObject taskObj, String taskId) {
        List<String> recipeTypes = new ArrayList<>();

        try {
            if (taskObj.has("recipe_types") && taskObj.get("recipe_types").isJsonArray()) {
                JsonArray typesArray = taskObj.get("recipe_types").getAsJsonArray();
                for (JsonElement typeElement : typesArray) {
                    if (typeElement.isJsonPrimitive()) {
                        String type = normalizeRecipeType(typeElement.getAsString());
                        if (!type.isEmpty()) {
                            recipeTypes.add(type);
                        }
                    }
                }
            } else if (taskObj.has("recipes") && taskObj.get("recipes").isJsonArray()) {
                JsonArray recipesArray = taskObj.get("recipes").getAsJsonArray();
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
            } else {
                recipeTypes.add("crafting");
            }
        } catch (Exception e) {
            System.err.println("[ArsMelima] Error parsing recipe types for task " + taskId + ": " + e.getMessage());
            recipeTypes.add("crafting");
        }

        recipeTypes = new ArrayList<>(new LinkedHashSet<>(recipeTypes));
        return recipeTypes;
    }

    private static String normalizeRecipeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "";
        }
        return type.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
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
        return debugTasks;
    }
}