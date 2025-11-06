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
            System.out.println("[ArsMelima] loadTasks: empty learningChapterId");
            return createDebugTasks(learningChapterId); // сразу возвращаем debug задачи
        }

        System.out.println("[ArsMelima] === START loadTasks ===");
        System.out.println("[ArsMelima] Loading tasks for chapter: " + learningChapterId);

        List<String> langs = LearningChapterLoader.getLanguageCandidates();
        System.out.println("[ArsMelima] Language candidates: " + langs);

        boolean foundAnyFile = false;
        boolean foundValidTasks = false;

        for (String lang : langs) {
            String basePath = "__BASE__".equals(lang) ? TASKS_DIR : "lang/" + lang + "/" + TASKS_DIR;
            String filePath = basePath + "/" + learningChapterId + ".json";
            ResourceLocation rl = new ResourceLocation("wd", filePath);

            System.out.println("[ArsMelima] Checking: " + rl);

            try {
                Resource resource = manager.getResource(rl).orElse(null);
                if (resource == null) {
                    System.out.println("[ArsMelima] ❌ Tasks file not found: " + rl);
                    continue;
                }

                System.out.println("[ArsMelima] ✅ Found tasks file: " + rl);
                foundAnyFile = true;

                try (InputStream is = resource.open(); InputStreamReader reader = new InputStreamReader(is)) {
                    JsonElement rootEl = JsonParser.parseReader(reader);
                    System.out.println("[ArsMelima] JSON root element type: " + rootEl.getClass().getSimpleName());

                    JsonArray tasksArray = null;

                    // Определяем структуру JSON и извлекаем массив задач
                    if (rootEl.isJsonArray()) {
                        tasksArray = rootEl.getAsJsonArray();
                        System.out.println("[ArsMelima] Loading tasks from array format");
                    } else if (rootEl.isJsonObject()) {
                        JsonObject rootObj = rootEl.getAsJsonObject();
                        System.out.println("[ArsMelima] JSON object keys: " + rootObj.keySet());

                        // Проверяем соответствие learning_chapter если указано
                        if (rootObj.has("learning_chapter")) {
                            String declared = rootObj.get("learning_chapter").getAsString();
                            System.out.println("[ArsMelima] Found learning_chapter: " + declared);
                            if (!matchesChapterId(declared, learningChapterId)) {
                                System.out.println("[ArsMelima] ❌ Chapter mismatch: '" + declared + "' != '" + learningChapterId + "'");
                                continue;
                            }
                        }

                        // Извлекаем массив задач из поля "tasks"
                        if (rootObj.has("tasks") && rootObj.get("tasks").isJsonArray()) {
                            tasksArray = rootObj.getAsJsonArray("tasks");
                            System.out.println("[ArsMelima] ✅ Loading tasks from object format");
                        } else if (looksLikeTaskObject(rootObj)) {
                            tasksArray = new JsonArray();
                            tasksArray.add(rootObj);
                            System.out.println("[ArsMelima] Loading single task from object");
                        } else {
                            System.out.println("[ArsMelima] ❌ Unsupported JSON structure");
                            continue;
                        }
                    } else {
                        System.out.println("[ArsMelima] ❌ Unexpected JSON element");
                        continue;
                    }

                    // Парсим массив задач
                    int parsedTasks = 0;
                    System.out.println("[ArsMelima] Tasks array size: " + tasksArray.size());

                    for (JsonElement element : tasksArray) {
                        if (!element.isJsonObject()) {
                            System.out.println("[ArsMelima] Skipping non-object element");
                            continue;
                        }

                        JsonObject taskObj = element.getAsJsonObject();
                        System.out.println("[ArsMelima] Task object keys: " + taskObj.keySet());

                        String id = taskObj.has("id") ? taskObj.get("id").getAsString() : "";
                        String itemId = taskObj.has("item") ? taskObj.get("item").getAsString() :
                                (taskObj.has("item_id") ? taskObj.get("item_id").getAsString() : "");
                        int count = taskObj.has("count") ? taskObj.get("count").getAsInt() :
                                (taskObj.has("required") ? taskObj.get("required").getAsInt() : 0);

                        System.out.println("[ArsMelima] Parsed task data - id: '" + id + "', item: '" + itemId + "', count: " + count);

                        // Валидация обязательных полей
                        if (id.isEmpty() || itemId.isEmpty() || count <= 0) {
                            System.out.println("[ArsMelima] ❌ Skipping invalid task");
                            continue;
                        }

                        // Поддержка нескольких типов рецептов
                        List<String> recipeTypes = parseRecipeTypes(taskObj, id);

                        // ВРЕМЕННОЕ ИСПРАВЛЕНИЕ: принудительно устанавливаем типы для bread_3
                        if (id.equals("bread_3")) {
                            System.out.println("[ArsMelima] 🎯 Applying forced recipe types for bread_3");
                            recipeTypes = Arrays.asList("crafting", "smelting", "campfire_cooking", "smoking", "stonecutting");
                        }

                        if (recipeTypes.isEmpty()) {
                            System.out.println("[ArsMelima] ⚠️ No recipe types, using fallback");
                            recipeTypes.add("crafting");
                        }

                        // Создаем задачу
                        Task task = new Task(id, itemId, count, recipeTypes, learningChapterId);
                        tasks.add(task);
                        parsedTasks++;
                        foundValidTasks = true;

                        System.out.println("[ArsMelima] ✅ Loaded task: " + id + " - " + itemId + " x" + count + " types: " + recipeTypes);
                    }

                    System.out.println("[ArsMelima] Successfully parsed " + parsedTasks + " tasks from " + rl);

                } catch (Exception e) {
                    System.err.println("[ArsMelima] ❌ Failed to parse tasks " + rl + " : " + e.getMessage());
                    e.printStackTrace();
                }

                // Если нашли задачи - останавливаем поиск
                if (!tasks.isEmpty()) {
                    System.out.println("[ArsMelima] 🎯 Successfully loaded " + tasks.size() + " tasks, stopping search");
                    break;
                }

            } catch (Exception e) {
                System.out.println("[ArsMelima] ❌ Tasks file error for " + rl + " : " + e.getMessage());
            }
        }

        System.out.println("[ArsMelima] Search results - foundAnyFile: " + foundAnyFile + ", foundValidTasks: " + foundValidTasks);

        // Fallback: проверка базовой папки если не нашли в локализованных
        if (!foundValidTasks && tasks.isEmpty()) {
            System.out.println("[ArsMelima] 🔍 No tasks found in localized folders, checking base folder...");
            tasks.addAll(loadFromBaseFolder(manager, learningChapterId));
        }

        // Final fallback: debug tasks
        if (tasks.isEmpty()) {
            System.out.println("[ArsMelima] 🛠️ No tasks found for " + learningChapterId + ", creating debug tasks");
            tasks.addAll(createDebugTasks(learningChapterId));
        }

        System.out.println("[ArsMelima] === FINAL RESULT ===");
        System.out.println("[ArsMelima] Loaded " + tasks.size() + " tasks for chapter: " + learningChapterId);

        for (Task task : tasks) {
            System.out.println("[ArsMelima] 📋 Final task: " + task.getId() +
                    " -> " + task.getItemId() + " x" + task.getRequiredCount() +
                    " types: " + task.getRecipeTypes());
        }

        System.out.println("[ArsMelima] === END loadTasks ===");

        return tasks;
    }

    /**
     * Загрузка задач из базовой папки (без локализации)
     */
    private static List<Task> loadFromBaseFolder(ResourceManager manager, String learningChapterId) {
        List<Task> tasks = new ArrayList<>();
        String baseFilePath = TASKS_DIR + "/" + learningChapterId + ".json";
        ResourceLocation baseRl = new ResourceLocation("wd", baseFilePath);

        System.out.println("[ArsMelima] 🔍 Checking base folder: " + baseRl);

        try {
            Resource baseResource = manager.getResource(baseRl).orElse(null);
            if (baseResource != null) {
                System.out.println("[ArsMelima] ✅ Found base tasks file: " + baseRl);

                try (InputStream is = baseResource.open(); InputStreamReader reader = new InputStreamReader(is)) {
                    JsonElement rootEl = JsonParser.parseReader(reader);
                    JsonArray tasksArray = null;

                    if (rootEl.isJsonArray()) {
                        tasksArray = rootEl.getAsJsonArray();
                        System.out.println("[ArsMelima] Base: Array format");
                    } else if (rootEl.isJsonObject() && rootEl.getAsJsonObject().has("tasks")) {
                        tasksArray = rootEl.getAsJsonObject().getAsJsonArray("tasks");
                        System.out.println("[ArsMelima] Base: Object with tasks array");
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

                                // ВРЕМЕННОЕ ИСПРАВЛЕНИЕ для base folder
                                if (id.equals("bread_3")) {
                                    recipeTypes = Arrays.asList("crafting", "smelting", "campfire_cooking", "smoking", "stonecutting");
                                }

                                Task task = new Task(id, itemId, count, recipeTypes, learningChapterId);
                                tasks.add(task);
                                System.out.println("[ArsMelima] ✅ Loaded from base: " + id + " - " + itemId + " types: " + recipeTypes);
                            }
                        }
                    }
                }
            } else {
                System.out.println("[ArsMelima] ❌ Base tasks file not found: " + baseRl);
            }
        } catch (Exception e) {
            System.out.println("[ArsMelima] ❌ Base tasks file error: " + e.getMessage());
        }

        return tasks;
    }

    /**
     * Парсинг типов рецептов из JSON объекта задачи
     */
    private static List<String> parseRecipeTypes(JsonObject taskObj, String taskId) {
        List<String> recipeTypes = new ArrayList<>();

        try {
            System.out.println("[ArsMelima] 🔍 Parsing recipe types for task: " + taskId);
            System.out.println("[ArsMelima] Task object has recipe_types: " + taskObj.has("recipe_types"));
            System.out.println("[ArsMelima] Task object has recipes: " + taskObj.has("recipes"));
            System.out.println("[ArsMelima] Task object has recipe_type: " + taskObj.has("recipe_type"));

            if (taskObj.has("recipe_types") && taskObj.get("recipe_types").isJsonArray()) {
                JsonArray typesArray = taskObj.get("recipe_types").getAsJsonArray();
                System.out.println("[ArsMelima] Found recipe_types array with " + typesArray.size() + " elements");

                for (JsonElement typeElement : typesArray) {
                    if (typeElement.isJsonPrimitive()) {
                        String type = normalizeRecipeType(typeElement.getAsString());
                        if (!type.isEmpty()) {
                            recipeTypes.add(type);
                            System.out.println("[ArsMelima]   Added recipe type: " + type);
                        }
                    }
                }

            } else if (taskObj.has("recipes") && taskObj.get("recipes").isJsonArray()) {
                JsonArray recipesArray = taskObj.get("recipes").getAsJsonArray();
                System.out.println("[ArsMelima] Found recipes array with " + recipesArray.size() + " elements");

                for (JsonElement recipeElement : recipesArray) {
                    if (recipeElement.isJsonObject()) {
                        JsonObject recipeObj = recipeElement.getAsJsonObject();
                        if (recipeObj.has("type") && recipeObj.get("type").isJsonPrimitive()) {
                            String type = normalizeRecipeType(recipeObj.get("type").getAsString());
                            if (!type.isEmpty()) {
                                recipeTypes.add(type);
                                System.out.println("[ArsMelima]   Added recipe type: " + type);
                            }
                        }
                    }
                }

            } else if (taskObj.has("recipe_type") || taskObj.has("recipeType")) {
                String recipeType = taskObj.has("recipe_type") ? taskObj.get("recipe_type").getAsString() :
                        taskObj.get("recipeType").getAsString();
                String normalizedType = normalizeRecipeType(recipeType);
                recipeTypes.add(normalizedType);
                System.out.println("[ArsMelima] Found single recipe type: " + normalizedType);
            } else {
                System.out.println("[ArsMelima] ⚠️ No recipe types found, using default");
                recipeTypes.add("crafting");
            }

        } catch (Exception e) {
            System.err.println("[ArsMelima] ❌ Error parsing recipe types for task " + taskId + ": " + e.getMessage());
            recipeTypes.add("crafting");
        }

        // Убираем дубликаты
        recipeTypes = new ArrayList<>(new LinkedHashSet<>(recipeTypes));
        System.out.println("[ArsMelima] Final recipe types for " + taskId + ": " + recipeTypes);

        return recipeTypes;
    }

    /**
     * Нормализация типа рецепта
     */
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

    /**
     * Создание тестовых задач для отладки
     */
    private static List<Task> createDebugTasks(String chapterId) {
        List<Task> debugTasks = new ArrayList<>();



        System.out.println("[ArsMelima] 🛠️ Created DEBUG task: bread_3 with types: " + debugTasks.get(0).getRecipeTypes());

        return debugTasks;
    }
}