package ru.imaginaerum.wd.client.gui.ars_melima;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressionLoader;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaInputHandler;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ArsMelimaUIManager;

public class ArsMelimaScreen extends Screen {

    private final ArsMelimaUIManager uiManager;
    private final ArsMelimaInputHandler inputHandler;
    private final ArsMelimaMenu menu;
    private ItemStack book;

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

        // 1) Chapters first — заполняется progressionIdIndex внутри menu
        var chapters = ChapterLoader.loadChapters();
        menu.setChapters(chapters);
        System.out.println("[ArsMelima] Chapters loaded: " + (chapters != null ? chapters.size() : 0));

        // начальное состояние
        menu.setCurrentIndex(-1);
        uiManager.setCurrentChapterPage(0);
        uiManager.setCurrentTextPage(0);
        uiManager.setCurrentProgressPage(0);

        // 2) Then progression nodes
        var nodes = ProgressionLoader.loadNodes();
        menu.setProgressNodes(nodes);
        System.out.println("[ArsMelima] Progression nodes loaded: " + (nodes != null ? nodes.size() : 0));

        // Небольшая диагностика: покажем первые несколько id (удобно при отладке)
        if (nodes != null && !nodes.isEmpty()) {
            for (int i = 0; i < Math.min(5, nodes.size()); i++) {
                System.out.println("[ArsMelima] node[" + i + "] id='" + nodes.get(i).getId() + "'");
            }
        }
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
        return inputHandler.handleMouseClick(mouseX, mouseY, button, uiManager, menu, book)
                || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}