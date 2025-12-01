package ru.imaginaerum.wd.common.ars_melima_core.skills_eat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressionLoader;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ProgressionUnlockManager;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber
public class FishDietEffectHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onEatFish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem();
        if (!isEdibleFish(stack)) return;

        List<ProgressNode> nodes = ProgressionLoader.loadNodes();
        ProgressNode fishDiet = null;

        for (ProgressNode n : nodes) {
            if ("fish_diet".equals(n.getId())) {
                fishDiet = n;
                break;
            }
        }

        if (fishDiet == null) return;

        if (!ProgressionUnlockManager.isUnlocked(player, "fish_diet")) return;

        if (RANDOM.nextFloat() > 0.20f) return;

        int level = Math.max(1, fishDiet.getLevel());

        int duration = 20 * 12;

        // Усиление зависит от уровня (уровень 1 = амплифаер 0)
        int amplifier = level - 1;

        player.addEffect(new MobEffectInstance(
                MobEffects.DOLPHINS_GRACE,
                duration,
                amplifier,
                false,
                true
        ));
    }

    private static boolean isEdibleFish(ItemStack stack) {
        return stack.is(Items.COD)
                || stack.is(Items.SALMON)
                || stack.is(Items.TROPICAL_FISH)
                || stack.is(Items.PUFFERFISH)
                || stack.is(Items.COOKED_COD)
                || stack.is(Items.COOKED_SALMON);
    }
}