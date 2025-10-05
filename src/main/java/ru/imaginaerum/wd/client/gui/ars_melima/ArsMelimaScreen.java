package ru.imaginaerum.wd.client.gui.ars_melima;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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
        menu.setChapters(ChapterLoader.loadChapters());
        menu.setCurrentIndex(-1);
        uiManager.setCurrentChapterPage(0);
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