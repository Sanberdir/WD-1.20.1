package ru.imaginaerum.wd.common.ars_melima_core.skills_eat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressionLoader;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ProgressionUnlockManager;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.List;

@Mod.EventBusSubscriber(modid = "wd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LeafLoverHandler {

    @SubscribeEvent
    public static void onStartEating(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem();

        // Проверяем, вегетарианская ли это пища
        if (!isVegetarianFood(stack)) return;

        // Проверяем, открыт ли навык lover_lean
        if (!ProgressionUnlockManager.isUnlocked(player, "lover_lean")) return;

        // Получаем уровень навыка
        ProgressNode leafLoverNode = findNode("lover_lean");
        if (leafLoverNode == null) return;

        int level = Math.max(1, leafLoverNode.getLevel());

        // Уменьшаем время использования на 10% за уровень
        // Базовое время в тиках (обычно 32 тика = 1.6 секунды)
        int originalDuration = event.getDuration();
        float reduction = 0.10f * level; // 10% за уровень
        int newDuration = (int) (originalDuration * (1.0f - Math.min(0.8f, reduction))); // Макс 80% ускорение

        event.setDuration(newDuration);
    }

    /**
     * Проверяет, является ли пища вегетарианской (без мяса/рыбы)
     */
    private static boolean isVegetarianFood(ItemStack stack) {
        return !isMeatOrFish(stack) && isEdibleFood(stack);
    }

    /**
     * Проверка на мясо или рыбу
     */
    private static boolean isMeatOrFish(ItemStack stack) {
        return stack.is(Items.BEEF)
                || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.PORKCHOP)
                || stack.is(Items.COOKED_PORKCHOP)
                || stack.is(Items.CHICKEN)
                || stack.is(Items.COOKED_CHICKEN)
                || stack.is(Items.MUTTON)
                || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.RABBIT)
                || stack.is(Items.COOKED_RABBIT)
                || stack.is(Items.RABBIT_STEW)
                || stack.is(Items.COD)
                || stack.is(Items.COOKED_COD)
                || stack.is(Items.SALMON)
                || stack.is(Items.COOKED_SALMON)
                || stack.is(Items.TROPICAL_FISH)
                || stack.is(Items.PUFFERFISH)
                || stack.is(Items.ROTTEN_FLESH)
                // Farmer's Delight мясные блюда
                || isFDMeatFood(stack);
    }

    /**
     * Проверка мясных блюд из Farmer's Delight
     */
    private static boolean isFDMeatFood(ItemStack stack) {
        try {
            return stack.is(ModItems.HAM.get())
                    || stack.is(ModItems.SMOKED_HAM.get())
                    || stack.is(ModItems.BACON.get())
                    || stack.is(ModItems.BACON_SANDWICH.get())
                    || stack.is(ModItems.CHICKEN_SANDWICH.get())
                    || stack.is(ModItems.HAMBURGER.get())
                    || stack.is(ModItems.MUTTON_WRAP.get())
                    || stack.is(ModItems.DUMPLINGS.get())
                    || stack.is(ModItems.STUFFED_POTATO.get())
                    || stack.is(ModItems.CABBAGE_ROLLS.get())
                    || stack.is(ModItems.BACON_AND_EGGS.get())
                    || stack.is(ModItems.CHICKEN_SOUP.get())
                    || stack.is(ModItems.FISH_STEW.get())
                    || stack.is(ModItems.BAKED_COD_STEW.get())
                    || stack.is(ModItems.BEEF_STEW.get());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверка, что предмет вообще является едой
     */
    private static boolean isEdibleFood(ItemStack stack) {
        return stack.getItem().getFoodProperties() != null;
    }

    private static ProgressNode findNode(String id) {
        List<ProgressNode> nodes = ProgressionLoader.loadNodes();
        for (ProgressNode n : nodes) {
            if (id.equals(n.getId())) return n;
        }
        return null;
    }
}