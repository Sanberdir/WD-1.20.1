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
public class HealthyEatingHandler {

    private static final Random RANDOM = new Random();
    private static final int EFFECT_DURATION = 20 * 60; // 1 минута

    @SubscribeEvent
    public static void onEatFood(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem();

        if (!ProgressionUnlockManager.isUnlocked(player, "healthy_eating")) return;

        ProgressNode healthyEating = findNode("healthy_eating");
        if (healthyEating == null) return;

        int level = Math.max(1, healthyEating.getLevel());

        if (isGreenFood(stack) && RANDOM.nextFloat() < 0.16f) {
            applyEffect(player, MobEffects.MOVEMENT_SPEED, level);
        } else if (isJuiceOrJam(stack) && RANDOM.nextFloat() < 0.15f) {
            applyEffect(player, MobEffects.DIG_SPEED, level);
        } else if (isOtherVegetarianFood(stack) && RANDOM.nextFloat() < 0.12f) {
            applyEffect(player, MobEffects.DAMAGE_BOOST, level);
        }
    }

    private static void applyEffect(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, int level) {
        player.addEffect(new MobEffectInstance(
                effect,
                EFFECT_DURATION,
                level - 1, // амплитуда = уровень - 1
                false,
                true
        ));
    }

    private static boolean isGreenFood(ItemStack stack) {
        return stack.is(Items.CARROT)
                || stack.is(Items.POTATO)
                || stack.is(Items.BEETROOT)
                || stack.is(Items.MELON_SLICE)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES)
                || stack.is(Items.DRIED_KELP)
                || stack.is(Items.SEA_PICKLE)
                || stack.is(Items.KELP)
                || stack.is(ModItems.FRUIT_SALAD.get())
                || stack.is(ModItems.CABBAGE.get());
    }

    private static boolean isJuiceOrJam(ItemStack stack) {
        return stack.is(Items.HONEY_BOTTLE)
                || stack.is(Items.COOKIE)
                || stack.is(Items.PUMPKIN_PIE)
                || stack.is(Items.CAKE)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES);
    }

    private static boolean isOtherVegetarianFood(ItemStack stack) {
        return !isMeatOrFish(stack)
                && !isGreenFood(stack)
                && !isJuiceOrJam(stack)
                && isEdibleFood(stack);
    }

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
                || stack.is(Items.ROTTEN_FLESH);
    }

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
