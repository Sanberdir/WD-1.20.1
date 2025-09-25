package ru.imaginaerum.wd.client.gui.ars_melima;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Загружает все json-файлы из resources/lang/<locale>/ars_melima и превращает их в Chapter.
 * Безопасно пытается определить код языка (поддерживает разные реализации LanguageManager.getSelected()).
 */
public class ChapterLoader {
    private static final Gson GSON = new Gson();

    // базовая папка внутри ресурсов, без конечного '/': мы будем искать lang/<code>/ars_melima
    private static final String BASE = "lang";
    private static final String SUBDIR = "ars_melima";

    public static List<Chapter> loadChapters() {
        List<Chapter> out = new ArrayList<>();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        // Получаем список кандидатов языковых кодов (в порядке приоритета)
        List<String> langs = getLanguageCandidates();

        // Для дедупликации по ресурсу
        Set<String> seen = new HashSet<>();

        for (String lang : langs) {
            String basePath;
            if ("__BASE__".equals(lang)) {
                // старый путь lang/ars_melima
                basePath = "lang/" + SUBDIR;
            } else {
                basePath = "lang/" + lang + "/" + SUBDIR;
            }

            try {
                Map<ResourceLocation, Resource> found =
                        manager.listResources(basePath, rl -> rl.getPath().endsWith(".json"));

                if (found == null || found.isEmpty()) continue;

                for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
                    ResourceLocation rl = entry.getKey();
                    String uniqueKey = rl.toString();
                    if (seen.contains(uniqueKey)) continue;
                    seen.add(uniqueKey);

                    Resource resource = entry.getValue();
                    try (InputStream is = resource.open();
                         InputStreamReader reader = new InputStreamReader(is)) {
                        JsonObject jo = JsonParser.parseReader(reader).getAsJsonObject();
                        String title = jo.has("title") ? jo.get("title").getAsString() : rl.getPath();
                        String content = jo.has("content") ? jo.get("content").getAsString() : "";
                        String image = jo.has("image") ? jo.get("image").getAsString() : null;

                        // id — имя файла без пути, например "chapter1.json"
                        String path = rl.getPath(); // e.g. lang/en_us/ars_melima/chapter1.json
                        String id;
                        int lastSlash = path.lastIndexOf('/');
                        if (lastSlash >= 0 && lastSlash + 1 < path.length()) {
                            id = path.substring(lastSlash + 1);
                        } else {
                            id = path;
                        }

                        out.add(new Chapter(id, title, content, image));
                    } catch (Exception e) {
                        System.err.println("[ArsMelima] Failed to load chapter resource " + rl + " : " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // Если нашли хоть какие-то главы для этой локали — возвращаем их
                if (!out.isEmpty()) {
                    return out;
                }
            } catch (Exception e) {
                System.err.println("[ArsMelima] Error while listing/loading chapter resources in " + basePath + " : " + e.getMessage());
                e.printStackTrace();
                // пробуем дальше по списку кандидатов
            }
        }

        // ничего не нашли — возвращаем пустой список
        return out;
    }

    private static List<String> getLanguageCandidates() {
        List<String> langs = new ArrayList<>();
        try {
            // 1) Попытка взять выбранный язык из LanguageManager.getSelected()
            try {
                Object sel = null;
                try {
                    sel = Minecraft.getInstance().getLanguageManager().getSelected();
                } catch (Throwable t) {
                    // на всякий случай — если API изменилось или недоступно
                    sel = null;
                }

                if (sel != null) {
                    if (sel instanceof String) {
                        String code = (String) sel;
                        if (!code.isEmpty()) langs.add(normalizeLangCode(code));
                    } else {
                        // попробуем через рефлексию получить метод getCode()
                        try {
                            Method m = sel.getClass().getMethod("getCode");
                            Object codeObj = m.invoke(sel);
                            if (codeObj instanceof String) {
                                String code = (String) codeObj;
                                if (!code.isEmpty()) langs.add(normalizeLangCode(code));
                            }
                        } catch (NoSuchMethodException ignored) {
                            // ничего — некоторые реализации не имеют getCode()
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            // 2) Системная локаль Java
            try {
                Locale locale = Locale.getDefault();
                if (locale != null) {
                    String tag = locale.toString(); // e.g. en_US
                    if (tag != null && !tag.isEmpty()) langs.add(normalizeLangCode(tag));

                    String language = locale.getLanguage(); // e.g. en
                    if (language != null && !language.isEmpty()) langs.add(normalizeLangCode(language));

                    String langCountry = locale.getLanguage() + "_" + locale.getCountry(); // en_US
                    if (langCountry != null && !langCountry.isEmpty()) langs.add(normalizeLangCode(langCountry));
                }
            } catch (Throwable ignored) { }
        } finally {
            // гарантированные fallback'ы
            langs.add("en_us");
            langs.add("ru_ru");
            // специальный маркер для старого/универсального пути lang/ars_melima
            langs.add("__BASE__");
        }

        // Уберём дубли, сохранив порядок
        LinkedHashSet<String> uniq = new LinkedHashSet<>(langs);
        return new ArrayList<>(uniq);
    }

    private static String normalizeLangCode(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        // если это всего 2 буквы, оставим как есть (например "en")
        return s;
    }
}
