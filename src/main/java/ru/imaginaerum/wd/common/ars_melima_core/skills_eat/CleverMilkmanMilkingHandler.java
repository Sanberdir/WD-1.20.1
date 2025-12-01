package ru.imaginaerum.wd.common.ars_melima_core.skills_eat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ProgressionUnlockManager;
import vectorwing.farmersdelight.common.registry.ModItems;

@Mod.EventBusSubscriber(modid = "wd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CleverMilkmanMilkingHandler {

    @SubscribeEvent
    public static void onInteractWithCow(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() == null || event.getEntity() == null) return;

        if (!(event.getTarget() instanceof Cow cow)) return;
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!stack.is(Items.GLASS_BOTTLE)) return;
        if (!ProgressionUnlockManager.isUnlocked(player, "clever_milkman")) return;
        if (cow.isBaby()) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));

        // Анимация руки
        if (player instanceof ServerPlayer serverPlayer) {
            // Для серверного игрока отправляем пакет всем для анимации
            serverPlayer.swing(event.getHand(), true);
        } else {
            // Для клиентского игрока просто вызываем анимацию
            player.swing(event.getHand());
        }

        if (!player.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            ItemStack milkBottle = new ItemStack(ModItems.MILK_BOTTLE.get());
            if (!player.getInventory().add(milkBottle)) {
                player.drop(milkBottle, false);
            }

            player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.COW_MILK,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            System.out.println("[CleverMilkman] Cow milked with bottle by " + player.getName().getString());
        }
    }
}