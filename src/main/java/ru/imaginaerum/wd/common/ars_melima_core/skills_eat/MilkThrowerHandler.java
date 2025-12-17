package ru.imaginaerum.wd.common.ars_melima_core.skills_eat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ProgressionUnlockManager;
import ru.imaginaerum.wd.common.entities.ModEntities;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.Collection;
import java.util.List;

@Mod.EventBusSubscriber(modid = "wd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MilkThrowerHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity(); // Используем Player, а не LivingEntity
        ItemStack stack = event.getItemStack();

        // Проверяем, что в руке бутылка молока и есть навык milk_thrower
        if (!stack.is(ModItems.MILK_BOTTLE.get())) return;
        // Времено без ! спереди
        if (!ProgressionUnlockManager.isUnlocked(player, "milk_thrower")) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));

        // Анимация броска
        player.swing(event.getHand());

        if (!player.level().isClientSide) {
            // Создаем снаряд-бутылку молока
            MilkBottleProjectile bottle = new MilkBottleProjectile(player.level(), player);
            bottle.setItem(stack);
            bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            player.level().addFreshEntity(bottle);

            // Уменьшаем стак, если не в креативе
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            // Звук броска
            player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.SPLASH_POTION_THROW,
                    SoundSource.PLAYERS,
                    0.5F,
                    0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F)
            );
        }
    }

    // Класс снаряда для бутылки молока
    public static class MilkBottleProjectile extends ThrowableItemProjectile {
        public MilkBottleProjectile(EntityType<? extends ThrowableItemProjectile> type, net.minecraft.world.level.Level level) {
            super(type, level);
        }

        public MilkBottleProjectile(net.minecraft.world.level.Level level, LivingEntity shooter) {
            super(ModEntities.MILK_BOTTLE.get(), shooter, level);
        }

        public MilkBottleProjectile(net.minecraft.world.level.Level level, double x, double y, double z) {
            super(ModEntities.MILK_BOTTLE.get(), x, y, z, level);
        }

        @Override
        protected Item getDefaultItem() {
            return ModItems.MILK_BOTTLE.get();
        }

        @Override
        protected void onHit(HitResult result) {
            super.onHit(result);

            if (!this.level().isClientSide) {
                // Создаем частицы как у зелья
                this.level().playSound(
                        null,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.GLASS_BREAK,
                        SoundSource.NEUTRAL,
                        0.5F,
                        0.4F / (this.random.nextFloat() * 0.4F + 0.8F)
                );

                // Спавним частицы на сервере
                ServerLevel serverLevel = (ServerLevel) this.level();
                int particleCount = 8;
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 0.5;
                    double offsetY = (random.nextDouble() - 0.5) * 0.5;
                    double offsetZ = (random.nextDouble() - 0.5) * 0.5;

                    serverLevel.sendParticles(
                            ParticleTypes.EFFECT,
                            this.getX() + offsetX,
                            this.getY() + offsetY,
                            this.getZ() + offsetZ,
                            1,
                            0, 0, 0,
                            0.1
                    );
                }

                // Находим все живые сущности в радиусе 2 блоков
                AABB area = new AABB(
                        this.getX() - 2.0, this.getY() - 2.0, this.getZ() - 2.0,
                        this.getX() + 2.0, this.getY() + 2.0, this.getZ() + 2.0
                );

                List<LivingEntity> entities = this.level().getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        entity -> entity.isAlive()
                );

                // Снимаем все эффекты с найденных сущностей
                for (LivingEntity entity : entities) {

                    // Получаем все активные эффекты
                    Collection<MobEffectInstance> activeEffects = entity.getActiveEffects();
                    if (!activeEffects.isEmpty()) {
                        // Копируем список, чтобы избежать ConcurrentModificationException
                        List<MobEffectInstance> effectsToRemove = new java.util.ArrayList<>(activeEffects);

                        // Удаляем каждый эффект
                        for (MobEffectInstance effect : effectsToRemove) {
                            entity.removeEffect(effect.getEffect());
                        }

                    }
                }

                // Удаляем снаряд
                this.discard();
            }
        }

        @Override
        protected void onHitEntity(EntityHitResult result) {
            // Дополнительная логика при попадании в сущность
            super.onHitEntity(result);
        }

        @Override
        protected void onHitBlock(BlockHitResult result) {
            // Дополнительная логика при попадании в блок
            super.onHitBlock(result);
        }

        @Override
        protected float getGravity() {
            return 0.05F; // Меньшая гравитация для лучшей броскаемости
        }
    }
}