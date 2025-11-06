package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public class Task {
    private final String id;
    private final String itemId;
    private final int requiredCount;
    private final List<String> recipeTypes;
    private final String chapterId; // новая глава

    public Task(String id, String itemId, int requiredCount, List<String> recipeTypes, String chapterId) {
        this.id = id;
        this.itemId = itemId;
        this.requiredCount = requiredCount;
        this.recipeTypes = new ArrayList<>(recipeTypes);
        this.chapterId = chapterId;
    }
    public Item getItem() {
        try {
            return BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
        } catch (Exception e) {
            return null;
        }
    }
    public String getChapterId() { return chapterId; }
    public String getId() { return id; }
    public String getItemId() { return itemId; }
    public int getRequiredCount() { return requiredCount; }
    public List<String> getRecipeTypes() { return Collections.unmodifiableList(recipeTypes); }

    public boolean matchesRecipeType(String type) {
        return recipeTypes.contains(type);
    }
}
