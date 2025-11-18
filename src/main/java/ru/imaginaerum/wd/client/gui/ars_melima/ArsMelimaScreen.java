package ru.imaginaerum.wd.client.gui.ars_melima;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressionLoader;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaInputHandler;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaUIManager;

import java.util.ArrayList;
import java.util.List;

public class ArsMelimaScreen extends Screen {

    private final ArsMelimaUIManager uiManager;
    private final ArsMelimaInputHandler inputHandler;
    private final ArsMelimaMenu menu;
    private final ItemStack book;

    public ArsMelimaScreen(ItemStack book) {
        super(Component.literal("Ars Melima"));
        this.book = book;
        this.menu = new ArsMelimaMenu();
        this.uiManager = new ArsMelimaUIManager();
        this.inputHandler = new ArsMelimaInputHandler();
    }

    @Override
    protected void init() {
        super.init();

        // 1) Загружаем главы
        var chapters = ChapterLoader.loadChapters();
        menu.setChapters(chapters);

        // Тест: создаем тестовые TreeLinks если их нет
        if (chapters != null && !chapters.isEmpty()) {
            String firstChapterId = chapters.get(0).getId();
            List<TreeLink> existingLinks = menu.getTreeLinks(firstChapterId);
            if (existingLinks == null || existingLinks.isEmpty()) {
                createTestTreeLinks(firstChapterId);
            }
        }

        // Инициализируем состояние UI
        menu.setCurrentIndex(-1);
        uiManager.setCurrentChapterPage(0);
        uiManager.setCurrentTextPage(0);
        uiManager.setCurrentProgressPage(0);

        // 2) Загружаем прогрессию
        var nodes = ProgressionLoader.loadNodes();
        menu.setProgressNodes(nodes);

        // Проверяем TreeLinkLoader
        checkTreeLinkLoader();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        uiManager.render(graphics, mouseX, mouseY, width, height, menu, book, font);

        RenderSystem.disableBlend();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = inputHandler.handleMouseClick(mouseX, mouseY, button, uiManager, menu, book);
        return handled || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ========== ДИАГНОСТИЧЕСКИЕ МЕТОДЫ ==========

    private void checkTreeLinkLoader() {
        try {
            Class.forName("ru.imaginaerum.wd.client.gui.ars_melima.TreeLinkLoader");
        } catch (ClassNotFoundException e) {
            createTemporaryTreeLinkLoader();
        }
    }

    private void createTemporaryTreeLinkLoader() {
        try {
            // Создаем временную реализацию TreeLinkLoader через рефлексию
            java.util.function.Function<String, List<TreeLink>> tempLoader = (chapterId) -> {
                // Создаем тестовые данные с правильным количеством параметров
                List<TreeLink> testLinks = new ArrayList<>();
                try {
                    // Пробуем разные варианты конструктора
                    Class<?> treeLinkClass = TreeLink.class;

                    // Вариант 1: 3 параметра (String, String, String)
                    java.lang.reflect.Constructor<?> constructor3 = treeLinkClass.getConstructor(String.class, String.class, String.class);
                    testLinks.add((TreeLink) constructor3.newInstance("test_link_1", "Test Link 1 - " + chapterId, ""));
                    testLinks.add((TreeLink) constructor3.newInstance("test_link_2", "Test Link 2 - " + chapterId, ""));
                    testLinks.add((TreeLink) constructor3.newInstance("test_link_3", "Test Link 3 - " + chapterId, ""));

                } catch (Exception e) {
                    // Пробуем вариант с 2 параметрами
                    try {
                        Class<?> treeLinkClass2 = TreeLink.class;
                        java.lang.reflect.Constructor<?> constructor2 = treeLinkClass2.getConstructor(String.class, String.class);
                        testLinks.add((TreeLink) constructor2.newInstance("test_link_1", "Test Link 1 - " + chapterId));
                        testLinks.add((TreeLink) constructor2.newInstance("test_link_2", "Test Link 2 - " + chapterId));
                        testLinks.add((TreeLink) constructor2.newInstance("test_link_3", "Test Link 3 - " + chapterId));
                    } catch (Exception e2) {
                        // Оставляем пустой список в случае ошибки
                    }
                }

                return testLinks;
            };

            // Инжектим в меню через рефлексию
            java.lang.reflect.Field cacheField = ArsMelimaMenu.class.getDeclaredField("treeLinksCache");
            cacheField.setAccessible(true);

            // Заменяем Function в кэше
            java.util.Map<String, List<TreeLink>> cache = new java.util.HashMap<String, List<TreeLink>>() {
                @Override
                public List<TreeLink> computeIfAbsent(String key, java.util.function.Function<? super String, ? extends List<TreeLink>> mappingFunction) {
                    return tempLoader.apply(key);
                }
            };

            cacheField.set(menu, cache);

        } catch (Exception e) {
            // Игнорируем ошибки при создании временного загрузчика
        }
    }

    private void createTestTreeLinks(String chapterId) {
        try {
            java.lang.reflect.Field cacheField = ArsMelimaMenu.class.getDeclaredField("treeLinksCache");
            cacheField.setAccessible(true);

            @SuppressWarnings("unchecked")
            java.util.Map<String, List<TreeLink>> cache =
                    (java.util.Map<String, List<TreeLink>>) cacheField.get(menu);

            List<TreeLink> testLinks = new ArrayList<>();

            // Создаем TreeLinks с правильным конструктором
            Class<?> treeLinkClass = TreeLink.class;

            try {
                // Пробуем конструктор с 3 параметрами
                java.lang.reflect.Constructor<?> constructor = treeLinkClass.getConstructor(String.class, String.class, String.class);
                testLinks.add((TreeLink) constructor.newInstance("test_1", "Test TreeLink 1", ""));
                testLinks.add((TreeLink) constructor.newInstance("test_2", "Test TreeLink 2", ""));
                testLinks.add((TreeLink) constructor.newInstance("test_3", "Test TreeLink 3", ""));
            } catch (Exception e) {
                // Оставляем пустой список в случае ошибки
            }

            cache.put(chapterId, testLinks);

        } catch (Exception e) {
            // Игнорируем ошибки при создании тестовых данных
        }
    }
}