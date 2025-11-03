package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public class Task {
    private final String id;
    private final String itemId;
    private final int requiredCount;
    private final List<String> recipeTypes; // Теперь список типов

    public Task(String id, String itemId, int requiredCount, String recipeType) {
        this(id, itemId, requiredCount, Collections.singletonList(recipeType));
    }

    public Task(String id, String itemId, int requiredCount, List<String> recipeTypes) {
        this.id = id;
        this.itemId = itemId;
        this.requiredCount = requiredCount;
        this.recipeTypes = new ArrayList<>(recipeTypes);
    }

    public String getId() { return id; }
    public String getItemId() { return itemId; }
    public int getRequiredCount() { return requiredCount; }
    public List<String> getRecipeTypes() { return Collections.unmodifiableList(recipeTypes); }

    // Для обратной совместимости
    public String getRecipeType() {
        return recipeTypes.isEmpty() ? "crafting" : recipeTypes.get(0);
    }

    public Item getItem() {
        try {
            return BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean matchesRecipeType(String type) {
        return recipeTypes.contains(type);
    }
}