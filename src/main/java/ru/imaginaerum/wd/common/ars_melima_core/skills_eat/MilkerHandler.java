package ru.imaginaerum.wd.common.ars_melima_core.skills_eat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ProgressionUnlockManager;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "wd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MilkerHandler {

    // Хранение времени последнего использования для каждого игрока
    private static final Map<UUID, Long> lastUseTime = new HashMap<>();
    private static final int COOLDOWN_TICKS = 20; // 1 секунда задержки (20 тиков = 1 секунда)

    @SubscribeEvent
    public static void onInteractWithCow(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() == null || event.getEntity() == null) return;

        if (!(event.getTarget() instanceof Cow cow)) return;
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!stack.is(Items.GLASS_BOTTLE)) return;
        if (!ProgressionUnlockManager.isUnlocked(player, "milker")) return;
        if (cow.isBaby()) return;

        // Проверка кулдауна
        UUID playerId = player.getUUID();
        long currentTime = player.level().getGameTime();
        if (lastUseTime.containsKey(playerId)) {
            long lastTime = lastUseTime.get(playerId);
            if (currentTime - lastTime < COOLDOWN_TICKS) {
                // Игрок слишком быстро пытается использовать
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);

                // Анимация отказа
                player.swing(event.getHand());

                // Звук отказа
                if (!player.level().isClientSide) {
                    player.level().playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.VILLAGER_NO,
                            SoundSource.PLAYERS,
                            0.5F,
                            1.0F
                    );
                }

                System.out.println("[CleverMilkman] Player " + player.getName().getString() +
                        " tried to milk too fast!");
                return;
            }
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));

        // Анимация руки (длинная анимация)
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.swing(event.getHand(), true);
        } else {
            player.swing(event.getHand());
        }

        if (!player.level().isClientSide) {
            // Устанавливаем время последнего использования
            lastUseTime.put(playerId, currentTime);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            ItemStack milkBottle = new ItemStack(ModItems.MILK_BOTTLE.get());
            if (!player.getInventory().add(milkBottle)) {
                player.drop(milkBottle, false);
            }

            // Звук доения
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

            // Дополнительный звук для обратной связи
            player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    0.3F,
                    Mth.randomBetween(player.getRandom(), 1.1F, 1.3F)
            );

            System.out.println("[CleverMilkman] Cow milked with bottle by " + player.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack stack = event.getItem();

        if (stack.is(ModItems.MILK_BOTTLE.get())) {
            if (!ProgressionUnlockManager.isUnlocked(player, "milker")) {
                event.setCanceled(true);

                // Анимация руки при попытке использовать без навыка
                player.swing(InteractionHand.MAIN_HAND);

                // Звук отказа
                if (!player.level().isClientSide) {
                    player.level().playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.VILLAGER_NO,
                            SoundSource.PLAYERS,
                            0.7F,
                            1.0F
                    );
                }

                System.out.println("[CleverMilkman] Player " + player.getName().getString() +
                        " tried to drink milk without milker skill!");
            }
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack stack = event.getItem();

        if (stack.is(ModItems.MILK_BOTTLE.get())) {
            if (!ProgressionUnlockManager.isUnlocked(player, "milker")) {
                // Возвращаем предмет и отменяем эффекты
                event.setResultStack(stack);

                // Дополнительная анимация
                player.swing(InteractionHand.MAIN_HAND);

                System.out.println("[CleverMilkman] Prevented milk consumption by " +
                        player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (stack.is(ModItems.MILK_BOTTLE.get())) {
            if (!ProgressionUnlockManager.isUnlocked(player, "milker")) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);

                // Анимация руки при попытке выпить
                player.swing(event.getHand());

                // Звук отказа
                if (!player.level().isClientSide) {
                    player.level().playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.VILLAGER_NO,
                            SoundSource.PLAYERS,
                            0.7F,
                            1.0F
                    );

                    // Можно добавить частицы для визуального эффекта
                    // spawnParticles(player.level(), player.position());
                }

                System.out.println("[CleverMilkman] Blocked milk bottle use by " +
                        player.getName().getString());
            }
        }
    }

    // Опционально: очистка старых записей из мапы (если нужно)
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Каждые 5 минут очищаем старые записи
            if (event.getServer().overworld().getGameTime() % 6000 == 0) {
                long currentTime = event.getServer().overworld().getGameTime();
                lastUseTime.entrySet().removeIf(entry ->
                        currentTime - entry.getValue() > 6000); // Удаляем записи старше 5 минут
            }
        }
    }

    // Опциональный метод для визуальных эффектов
    private static void spawnParticles(net.minecraft.world.level.Level level, net.minecraft.world.phys.Vec3 pos) {
        if (level.isClientSide) {
            for (int i = 0; i < 8; ++i) {
                double d0 = level.random.nextGaussian() * 0.02;
                double d1 = level.random.nextGaussian() * 0.02;
                double d2 = level.random.nextGaussian() * 0.02;
                level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                        pos.x + (double)(level.random.nextFloat() * 0.6F) - 0.3,
                        pos.y + 1.0 + (double)(level.random.nextFloat() * 0.6F),
                        pos.z + (double)(level.random.nextFloat() * 0.6F) - 0.3,
                        d0, d1, d2
                );
            }
        }
    }
}