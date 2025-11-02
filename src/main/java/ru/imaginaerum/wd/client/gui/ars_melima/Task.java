package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class Task {
    private final String id;
    private final String itemId; // ID предмета, например "minecraft:bread"
    private final int requiredCount;
    private final String recipeType; // тип рецепта: "crafting", "smelting", "smoking", "campfire_cooking", "stonecutting", "smithing"

    public Task(String id, String itemId, int requiredCount, String recipeType) {
        this.id = id;
        this.itemId = itemId;
        this.requiredCount = requiredCount;
        this.recipeType = recipeType;
    }

    public String getId() { return id; }
    public String getItemId() { return itemId; }
    public int getRequiredCount() { return requiredCount; }
    public String getRecipeType() { return recipeType; }

    public Item getItem() {
        try {
            return BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
        } catch (Exception e) {
            return null;
        }
    }
}