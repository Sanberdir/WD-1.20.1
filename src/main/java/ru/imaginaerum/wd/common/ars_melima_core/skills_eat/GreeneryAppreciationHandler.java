package ru.imaginaerum.wd.common.ars_melima_core.skills_eat;

import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;

import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressionLoader;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ProgressionUnlockManager;

import java.util.List;

@Mod.EventBusSubscriber(modid = "wd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GreeneryAppreciationHandler {

    @SubscribeEvent
    public static void onEatPlantFood(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem();

        // Убрана проверка FoodProperties - она может быть причиной проблемы
        // FoodProperties food = stack.getFoodProperties(player);
        // if (food == null) return;

        if (!isPlantFood(stack)) return;

        ProgressNode greenery = findNode("appreciate_greenery");
        if (greenery == null) return;

        if (!ProgressionUnlockManager.isUnlocked(player, "appreciate_greenery")) return;

        int level = Math.max(1, greenery.getLevel());

        // +1 голода за уровень
        int bonus = level;

        player.getFoodData().setFoodLevel(
                Math.min(20, player.getFoodData().getFoodLevel() + bonus)
        );

        player.getFoodData().setSaturation(
                Math.min(20f, player.getFoodData().getSaturationLevel() + bonus)
        );
    }

    private static boolean isPlantFood(ItemStack stack) {
        // Используем stack.is() как в рабочем классе FishDietEffectHandler
        return stack.is(Items.APPLE)
                || stack.is(Items.DRIED_KELP)
                || stack.is(Items.CARROT)
                || stack.is(Items.POTATO)
                || stack.is(Items.BEETROOT)
                || stack.is(Items.MELON_SLICE)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES);
    }

    private static ProgressNode findNode(String id) {
        List<ProgressNode> nodes = ProgressionLoader.loadNodes();
        for (ProgressNode n : nodes) {
            if (id.equals(n.getId())) return n;
        }
        return null;
    }
}