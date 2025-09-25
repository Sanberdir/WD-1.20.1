package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Загружает все json-файлы из resources/lang/ars_melima и превращает их в Chapter.
 */
public class ChapterLoader {
    private static final Gson GSON = new Gson();
    private static final String BASE = "lang/ars_melima";

    public static List<Chapter> loadChapters() {
        List<Chapter> out = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        try {
            // В 1.20.1 listResources принимает predicate по ResourceLocation
            Map<ResourceLocation, Resource> found = manager.listResources(BASE, rl -> rl.getPath().endsWith(".json"));
            for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
                ResourceLocation rl = entry.getKey();
                Resource resource = entry.getValue();
                try (InputStream is = resource.open();
                     InputStreamReader reader = new InputStreamReader(is)) {
                    JsonObject jo = JsonParser.parseReader(reader).getAsJsonObject();
                    String title = jo.has("title") ? jo.get("title").getAsString() : rl.getPath();
                    String content = jo.has("content") ? jo.get("content").getAsString() : "";
                    String image = jo.has("image") ? jo.get("image").getAsString() : null;

                    // id — имя файла без пути, например "chapter1.json"
                    String path = rl.getPath(); // e.g. lang/ars_melima/chapter1.json
                    String id;
                    int lastSlash = path.lastIndexOf('/');
                    if (lastSlash >= 0 && lastSlash + 1 < path.length()) {
                        id = path.substring(lastSlash + 1);
                    } else {
                        id = path;
                    }

                    out.add(new Chapter(id, title, content, image));
                } catch (Exception e) {
                    // Локально логируем проблемный файл, но продолжаем загрузку остальных
                    System.err.println("[ArsMelima] Failed to load chapter resource " + rl + " : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            // Общий catch: listResources или иные операции могли бросить исключение в некоторых окружениях
            System.err.println("[ArsMelima] Error while listing/loading chapter resources in " + BASE + " : " + e.getMessage());
            e.printStackTrace();
        }

        return out;
    }
}
