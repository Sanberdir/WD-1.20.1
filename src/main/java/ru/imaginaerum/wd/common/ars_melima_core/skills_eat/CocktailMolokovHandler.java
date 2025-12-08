package ru.imaginaerum.wd.common.ars_melima_core.skills_eat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ProgressionUnlockManager;
import ru.imaginaerum.wd.common.entities.ModEntities;
import ru.imaginaerum.wd.common.items.ItemsWD;

import java.util.*;

@Mod.EventBusSubscriber(modid = "wd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CocktailMolokovHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        // Проверяем четыре типа коктейлей
        boolean isCocktail = stack.is(ItemsWD.COCKTAIL_MOLOKOV.get());
        boolean isSpicyCocktail = stack.is(ItemsWD.SPICY_COCKTAIL_MOLOKOV.get());
        boolean isDesiccateCocktail = stack.is(ItemsWD.WITHERING_COCKTAIL_MOLOKOV.get());
        boolean isDisorientingCocktail = stack.is(ItemsWD.DISORIENTING_COCKTAIL_MOLOKOV.get());

        if (!isCocktail && !isSpicyCocktail && !isDesiccateCocktail && !isDisorientingCocktail) return;

        // Только один навык для всех типов
        if (!ProgressionUnlockManager.isUnlocked(player, "cocktail_molokov")) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));

        player.swing(event.getHand());

        if (!player.level().isClientSide) {
            // Определяем тип коктейля по предмету
            int cocktailType = 0; // 0=обычный, 1=острый, 2=иссушающий, 3=дезориентирующий
            if (isSpicyCocktail) cocktailType = 1;
            if (isDesiccateCocktail) cocktailType = 2;
            if (isDisorientingCocktail) cocktailType = 3;

            CocktailProjectile cocktail = new CocktailProjectile(player.level(), player, cocktailType);
            cocktail.setItem(stack);
            cocktail.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            player.level().addFreshEntity(cocktail);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

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

    public static class CocktailProjectile extends ThrowableItemProjectile {
        private final int cocktailType; // 0=обычный, 1=острый, 2=иссушающий, 3=дезориентирующий

        public CocktailProjectile(EntityType<? extends ThrowableItemProjectile> type, net.minecraft.world.level.Level level) {
            super(type, level);
            this.cocktailType = 0;
        }

        public CocktailProjectile(net.minecraft.world.level.Level level, LivingEntity shooter, int cocktailType) {
            super(ModEntities.COCKTAIL_MOLOKOV.get(), shooter, level);
            this.cocktailType = cocktailType;
        }

        public CocktailProjectile(net.minecraft.world.level.Level level, double x, double y, double z) {
            super(ModEntities.COCKTAIL_MOLOKOV.get(), x, y, z, level);
            this.cocktailType = 0;
        }

        @Override
        protected Item getDefaultItem() {
            switch (cocktailType) {
                case 1: return ItemsWD.SPICY_COCKTAIL_MOLOKOV.get();
                case 2: return ItemsWD.WITHERING_COCKTAIL_MOLOKOV.get();
                case 3: return ItemsWD.DISORIENTING_COCKTAIL_MOLOKOV.get();
                default: return ItemsWD.COCKTAIL_MOLOKOV.get();
            }
        }

        @Override
        protected void onHit(HitResult result) {
            super.onHit(result);

            if (!this.level().isClientSide) {
                createCloud(cocktailType);
                this.discard();
            }
        }

        private void createCloud(int type) {
            // Звук разбития
            float volume = 0.8F;
            if (type == 1) volume = 1.0F; // Острый
            if (type == 2 || type == 3) volume = 0.9F; // Иссушающий и Дезориентирующий

            this.level().playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.GLASS_BREAK,
                    SoundSource.NEUTRAL,
                    volume,
                    0.4F / (this.random.nextFloat() * 0.4F + 0.8F)
            );

            // Частицы (всегда белые)
            ServerLevel serverLevel = (ServerLevel) this.level();
            int particleCount = 32;

            for (int i = 0; i < particleCount; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 4.0;
                double offsetY = (random.nextDouble() - 0.5) * 4.0;
                double offsetZ = (random.nextDouble() - 0.5) * 4.0;
                double speedX = (random.nextDouble() - 0.5) * 0.1;
                double speedY = random.nextDouble() * 0.1;
                double speedZ = (random.nextDouble() - 0.5) * 0.1;

                serverLevel.sendParticles(
                        ParticleTypes.EFFECT, // Всегда белые частицы
                        this.getX() + offsetX,
                        this.getY() + offsetY,
                        this.getZ() + offsetZ,
                        1,
                        speedX, speedY, speedZ,
                        0.1
                );
            }

            // Создаем облако
            AreaEffectCloud cloud;

            if (type == 3) {
                // Дезориентирующий коктейль - особое облако с отслеживанием выхода
                cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ()) {
                    private final Set<Integer> entitiesInCloud = new HashSet<>();

                    @Override
                    public void tick() {
                        super.tick();

                        if (!this.level().isClientSide) {
                            AABB area = new AABB(
                                    this.getX() - this.getRadius(),
                                    this.getY() - this.getRadius(),
                                    this.getZ() - this.getRadius(),
                                    this.getX() + this.getRadius(),
                                    this.getY() + this.getRadius(),
                                    this.getZ() + this.getRadius()
                            );

                            List<LivingEntity> currentEntities = this.level().getEntitiesOfClass(
                                    LivingEntity.class,
                                    area,
                                    LivingEntity::isAlive
                            );

                            Set<Integer> currentIds = new HashSet<>();
                            for (LivingEntity entity : currentEntities) {
                                currentIds.add(entity.getId());
                            }

                            // Определяем, кто вышел из облака
                            Set<Integer> exitedEntities = new HashSet<>(entitiesInCloud);
                            exitedEntities.removeAll(currentIds);

                            // Находим сущности, которые вышли, и применяем эффекты
                            for (Integer entityId : exitedEntities) {
                                net.minecraft.world.entity.Entity entity = this.level().getEntity(entityId);
                                if (entity instanceof LivingEntity livingEntity) {
                                    // Применяем эффекты тошноты и слепоты на 1 минуту
                                    livingEntity.addEffect(new MobEffectInstance(
                                            MobEffects.CONFUSION, // Тошнота
                                            1200, // 60 секунд (1200 тиков)
                                            0     // Уровень 1
                                    ));

                                    livingEntity.addEffect(new MobEffectInstance(
                                            MobEffects.BLINDNESS, // Слепота
                                            1200, // 60 секунд (1200 тиков)
                                            0     // Уровень 1
                                    ));
                                }
                            }

                            // Обновляем список сущностей в облаке
                            entitiesInCloud.clear();
                            entitiesInCloud.addAll(currentIds);

                            // Снимаем эффекты с тех, кто всё ещё в облаке
                            for (LivingEntity entity : currentEntities) {
                                if (this.tickCount % 10 == 0) {
                                    List<MobEffectInstance> effectsToRemove =
                                            new java.util.ArrayList<>(entity.getActiveEffects());

                                    for (MobEffectInstance effect : effectsToRemove) {
                                        entity.removeEffect(effect.getEffect());
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void remove(RemovalReason reason) {
                        super.remove(reason);
                        // При уничтожении облака все сущности считаются вышедшими
                        if (!this.level().isClientSide) {
                            for (Integer entityId : entitiesInCloud) {
                                net.minecraft.world.entity.Entity entity = this.level().getEntity(entityId);
                                if (entity instanceof LivingEntity livingEntity) {
                                    livingEntity.addEffect(new MobEffectInstance(
                                            MobEffects.CONFUSION,
                                            1200,
                                            0
                                    ));

                                    livingEntity.addEffect(new MobEffectInstance(
                                            MobEffects.BLINDNESS,
                                            1200,
                                            0
                                    ));
                                }
                            }
                        }
                    }
                };

                cloud.setDuration(80); // 4 секунды для дезориентирующего

            } else {
                // Обычное облако для других типов коктейлей
                cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ()) {
                    @Override
                    public void tick() {
                        super.tick();

                        if (!this.level().isClientSide) {
                            AABB area = new AABB(
                                    this.getX() - this.getRadius(),
                                    this.getY() - this.getRadius(),
                                    this.getZ() - this.getRadius(),
                                    this.getX() + this.getRadius(),
                                    this.getY() + this.getRadius(),
                                    this.getZ() + this.getRadius()
                            );

                            List<LivingEntity> entities = this.level().getEntitiesOfClass(
                                    LivingEntity.class,
                                    area,
                                    LivingEntity::isAlive
                            );

                            for (LivingEntity entity : entities) {
                                // Всегда снимаем эффекты каждые 0.5 секунды
                                if (this.tickCount % 10 == 0) {
                                    List<MobEffectInstance> effectsToRemove =
                                            new java.util.ArrayList<>(entity.getActiveEffects());

                                    for (MobEffectInstance effect : effectsToRemove) {
                                        entity.removeEffect(effect.getEffect());
                                    }
                                }

                                // Эффекты в зависимости от типа коктейля
                                if (type == 1 && this.tickCount % 10 == 0) {
                                    // Острый: наносим урон 2hp/s
                                    entity.hurt(this.damageSources().indirectMagic(this, this.getOwner()), 1.0F);
                                }

                                if (type == 2 && this.tickCount % 20 == 0) {
                                    // Иссушающий: добавляем эффект иссушения (Wither)
                                    entity.addEffect(new MobEffectInstance(
                                            MobEffects.WITHER, // Эффект иссушения
                                            60, // Длительность 3 секунды (60 тиков)
                                            1   // Уровень 2 (усиленный эффект)
                                    ));
                                }
                            }
                        }
                    }
                };

                cloud.setDuration(100); // 5 секунд для обычных
            }

            cloud.setRadius(4.0F);
            cloud.setRadiusPerTick(-cloud.getRadius() / (float)cloud.getDuration());
            cloud.setParticle(ParticleTypes.EFFECT);
            cloud.setFixedColor(0xFFFFFF); // Всегда белый цвет

            this.level().addFreshEntity(cloud);
        }

        @Override
        protected float getGravity() {
            return 0.03F;
        }
    }
}