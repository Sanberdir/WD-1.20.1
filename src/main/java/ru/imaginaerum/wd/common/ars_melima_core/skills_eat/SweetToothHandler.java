package ru.imaginaerum.wd.common.ars_melima_core.skills_eat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import java.util.Random;

@Mod.EventBusSubscriber(modid = "wd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SweetToothHandler {

    @SubscribeEvent
    public static void onEatSweetFood(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem();

        if (!isSweetFood(stack)) return;
        if (!ProgressionUnlockManager.isUnlocked(player, "sweet_tooth")) return;

        ProgressNode sweetToothNode = findNode("sweet_tooth");
        if (sweetToothNode == null) return;

        int level = Math.max(1, sweetToothNode.getLevel());
        applySpeedEffect(player, level);
    }

    private static void applySpeedEffect(ServerPlayer player, int skillLevel) {
        // Длительность: 30 секунд для уровня 1, +3 секунды за уровень
        int duration = (30 + (skillLevel * 3)) * 20;

        // Уровень эффекта: I для уровня 1-4, II для 5-8, III для 9+
        int amplifier = Math.min(2, (skillLevel - 1) / 4);

        // Всегда даём эффект (100% шанс)
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                duration,
                amplifier,
                false,
                true
        ));
    }

    private static boolean isSweetFood(ItemStack stack) {
        return stack.is(Items.CAKE)
                || stack.is(Items.PUMPKIN_PIE)
                || stack.is(Items.COOKIE)
                || stack.is(Items.HONEY_BOTTLE)
                || isFarmerDelightSweetFood(stack);
    }

    private static boolean isFarmerDelightSweetFood(ItemStack stack) {
        // Пытаемся проверить каждый предмет, если он доступен
        try {
            return stack.is(ModItems.HOT_COCOA.get())
                    || stack.is(ModItems.PIE_CRUST.get())
                    || stack.is(ModItems.CAKE_SLICE.get())
                    || stack.is(ModItems.APPLE_PIE_SLICE.get())
                    || stack.is(ModItems.SWEET_BERRY_CHEESECAKE_SLICE.get())
                    || stack.is(ModItems.CHOCOLATE_PIE_SLICE.get())
                    || stack.is(ModItems.GLOW_BERRY_CUSTARD.get())
                    || stack.is(ModItems.MILK_BOTTLE.get());
        } catch (Exception e) {
            // Если какой-то предмет недоступен, просто пропускаем его
            return false;
        }
    }

    private static ProgressNode findNode(String id) {
        List<ProgressNode> nodes = ProgressionLoader.loadNodes();
        for (ProgressNode n : nodes) {
            if (id.equals(n.getId())) return n;
        }
        return null;
    }
}