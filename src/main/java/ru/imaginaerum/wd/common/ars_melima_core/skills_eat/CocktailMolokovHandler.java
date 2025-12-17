package ru.imaginaerum.wd.common.ars_melima_core.skills_eat;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
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
        if (!ProgressionUnlockManager.isUnlocked(player, "cocktail_molokov")) {
            // Анимация руки при отказе (нет навыка)
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
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);

            System.out.println("[CocktailMolokov] Player " + player.getName().getString() +
                    " tried to use cocktail without skill!");
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));

        // Анимация руки при успешном броске
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
        private static final UUID SLOW_UUID = UUID.fromString("1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d");

        // Глобальные коллекции для отслеживания
        private static final Set<Integer> entitiesInAnyCloud = new HashSet<>();
        private static final Map<Integer, Set<UUID>> entityCloudMap = new HashMap<>();

        public CocktailProjectile(EntityType<? extends ThrowableItemProjectile> type, net.minecraft.world.level.Level level) {
            super(type, level);
            this.cocktailType = 0;
        }
        private static EntityType<?> getEntityTypeByCocktail(int type) {
            return switch (type) {
                case 1 -> ModEntities.SPICY_COCKTAIL_MOLOKOV.get();               // spicy
                case 2 -> ModEntities.WITHERING_COCKTAIL_MOLOKOV.get();     // withering
                case 3 -> ModEntities.DISORIENTING_COCKTAIL_MOLOKOV.get();               // disorienting (если нет отдельного)
                default -> ModEntities.COCKTAIL_MOLOKOV.get();
            };
        }
        public CocktailProjectile(Level level, LivingEntity shooter, int cocktailType) {
            super((EntityType<? extends ThrowableItemProjectile>) getEntityTypeByCocktail(cocktailType), shooter, level);
            this.cocktailType = cocktailType;
            this.setItem(ItemStack.EMPTY); // ключевой момент
        }
        @Override
        public void handleEntityEvent(byte id) {
            if (id == 3) return; // не вызываем super — не будет белых частиц
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

            // Частицы — теперь цветные и совпадают с цветом облака
            ServerLevel serverLevel = (ServerLevel) this.level();
            int particleCount = 32;

// Цвет облака
            // Цвет частиц совпадает с цветом облака
            int fogColor = switch (type) {
                case 0 -> 0xFF6666; // обычный
                case 1 -> 0x66FFFF; // острый
                case 2 -> 0x333333; // иссушающий
                case 3 -> 0x99FF99; // дезориентирующий
                default -> 0xFFFFFF;
            };

// Переводим цвет в значения от 0 до 1
            float r = ((fogColor >> 16) & 0xFF) / 255f;
            float g = ((fogColor >> 8) & 0xFF) / 255f;
            float b = (fogColor & 0xFF) / 255f;

            float size = 1.0f; // размер частиц
            Vector3f colorVector = new Vector3f(r, g, b);
            DustParticleOptions dust = new DustParticleOptions(colorVector, size);

            for (int i = 0; i < particleCount; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 4.0;
                double offsetY = (random.nextDouble() - 0.5) * 4.0;
                double offsetZ = (random.nextDouble() - 0.5) * 4.0;
                double speedX = (random.nextDouble() - 0.5) * 0.1;
                double speedY = random.nextDouble() * 0.1;
                double speedZ = (random.nextDouble() - 0.5) * 0.1;

                serverLevel.sendParticles(
                        dust,
                        this.getX() + offsetX,
                        this.getY() + offsetY,
                        this.getZ() + offsetZ,
                        1,      // count
                        speedX,
                        speedY,
                        speedZ,
                        0.0     // extra
                );
            }


            // Создаем облако
            AreaEffectCloud cloud;

            if (type == 3) {
                // Дезориентирующий коктейль - особое облако с отслеживанием выхода
                cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ()) {
                    private final UUID cloudId = UUID.randomUUID();
                    private final Set<Integer> entitiesInThisCloud = new HashSet<>();

                    @Override
                    public void tick() {  // Исправлено: tick() вместо onTick()
                        super.tick();    // Исправлено: super.tick()

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

                            // Определяем, кто вошел в облако
                            Set<Integer> enteredEntities = new HashSet<>(currentIds);
                            enteredEntities.removeAll(entitiesInThisCloud);

                            // Определяем, кто вышел из облака
                            Set<Integer> exitedEntities = new HashSet<>(entitiesInThisCloud);
                            exitedEntities.removeAll(currentIds);

                            // Обрабатываем вошедших
                            for (Integer entityId : enteredEntities) {
                                registerEntityInCloud(entityId, cloudId);
                            }

                            // Обрабатываем вышедших
                            for (Integer entityId : exitedEntities) {
                                unregisterEntityFromCloud(entityId, cloudId);
                            }

                            // Обновляем список сущностей в этом облаке
                            entitiesInThisCloud.clear();
                            entitiesInThisCloud.addAll(currentIds);

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

                            // Для вышедших сущностей проверяем эффекты выхода
                            for (Integer entityId : exitedEntities) {
                                net.minecraft.world.entity.Entity entity = this.level().getEntity(entityId);
                                if (entity instanceof LivingEntity livingEntity) {
                                    // Если сущность больше не в любом облаке, применяем выходные эффекты
                                    if (!isEntityInAnyCloud(entityId)) {
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
                    }

                    @Override
                    public void remove(RemovalReason reason) {
                        // Обрабатываем выход всех сущностей при удалении облака
                        for (Integer entityId : new HashSet<>(entitiesInThisCloud)) {
                            unregisterEntityFromCloud(entityId, cloudId);
                            net.minecraft.world.entity.Entity entity = this.level().getEntity(entityId);
                            if (entity instanceof LivingEntity livingEntity) {
                                // Если сущность больше не в любом облаке, применяем выходные эффекты
                                if (!isEntityInAnyCloud(entityId)) {
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

                        super.remove(reason);
                    }
                };

                // ИЗМЕНЕНИЕ: Дезориентирующий коктейль теперь держится в 3 раза дольше (240 тиков вместо 80)
                cloud.setDuration(240); // 12 секунд вместо 4 секунд

            } else {
                // Обычное облако для других типов коктейлей
                cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ()) {
                    private final UUID cloudId = UUID.randomUUID();
                    private final Set<Integer> entitiesInThisCloud = new HashSet<>();

                    @Override
                    public void tick() {  // Исправлено: tick() вместо onTick()
                        super.tick();    // Исправлено: super.tick()

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

                            // Определяем, кто вошел в облако
                            Set<Integer> enteredEntities = new HashSet<>(currentIds);
                            enteredEntities.removeAll(entitiesInThisCloud);

                            // Определяем, кто вышел из облака
                            Set<Integer> exitedEntities = new HashSet<>(entitiesInThisCloud);
                            exitedEntities.removeAll(currentIds);

                            // Обрабатываем вошедших
                            for (Integer entityId : enteredEntities) {
                                registerEntityInCloud(entityId, cloudId);
                            }

                            // Обрабатываем вышедших
                            for (Integer entityId : exitedEntities) {
                                unregisterEntityFromCloud(entityId, cloudId);
                            }

                            // Обновляем список сущностей в этом облаке
                            entitiesInThisCloud.clear();
                            entitiesInThisCloud.addAll(currentIds);

                            for (LivingEntity entity : currentEntities) {
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
                                    entity.hurt(
                                            this.damageSources().indirectMagic(this, this.getOwner()),
                                            1.0F
                                    );
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

                    @Override
                    public void remove(RemovalReason reason) {
                        // Обрабатываем выход всех сущностей при удалении облака
                        for (Integer entityId : new HashSet<>(entitiesInThisCloud)) {
                            unregisterEntityFromCloud(entityId, cloudId);
                        }

                        super.remove(reason);
                    }
                };

                // ИЗМЕНЕНИЕ: Обычные коктейли теперь держатся в 3 раза дольше (300 тиков вместо 100)
                cloud.setDuration(300); // 15 секунд вместо 5 секунд
            }

            cloud.setRadius(4.0F);
            // ИЗМЕНЕНИЕ: Радиус должен уменьшаться медленнее, так как облако существует дольше
            cloud.setRadiusPerTick(-cloud.getRadius() / (float)cloud.getDuration());

            cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1, 0));

            cloud.setFixedColor(fogColor);
            cloud.setOwner((LivingEntity) this.getOwner());

            this.level().addFreshEntity(cloud);
        }

        @Override
        protected float getGravity() {
            return 0.03F;
        }

        // Глобальные методы для отслеживания облаков и замедления

        private static void registerEntityInCloud(int entityId, UUID cloudId) {


            // Добавляем облако к сущности
            entityCloudMap.computeIfAbsent(entityId, k -> new HashSet<>()).add(cloudId);

            // Добавляем сущность в общий список
            if (!entitiesInAnyCloud.contains(entityId)) {
                entitiesInAnyCloud.add(entityId);
                applySpeedModifierToEntity(entityId);
            }
        }

        private static void unregisterEntityFromCloud(int entityId, UUID cloudId) {
            // Удаляем облако из списка сущности
            Set<UUID> clouds = entityCloudMap.get(entityId);
            if (clouds != null) {
                clouds.remove(cloudId);
                if (clouds.isEmpty()) {
                    entityCloudMap.remove(entityId);
                }
            }

            // Проверяем, осталась ли сущность в других облаках
            if (!isEntityInAnyCloud(entityId)) {
                entitiesInAnyCloud.remove(entityId);
                removeSpeedModifierFromEntity(entityId);
            }
        }

        private static boolean isEntityInAnyCloud(int entityId) {
            Set<UUID> clouds = entityCloudMap.get(entityId);
            return clouds != null && !clouds.isEmpty();
        }

        private static void applySpeedModifierToEntity(int entityId) {
            // Откладываем применение до следующего тика сервера
            ServerTickHandler.scheduleSpeedModifierApply(entityId);
        }

        private static void removeSpeedModifierFromEntity(int entityId) {
            // Откладываем удаление до следующего тика сервера
            ServerTickHandler.scheduleSpeedModifierRemove(entityId);
        }
    }

    // Отдельный класс для обработки серверных тиков
    @Mod.EventBusSubscriber(modid = "wd", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerTickHandler {
        private static final Set<Integer> entitiesToSlow = new HashSet<>();
        private static final Set<Integer> entitiesToUnslow = new HashSet<>();
        private static final UUID SLOW_UUID = UUID.fromString("1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d");

        public static void scheduleSpeedModifierApply(int entityId) {
            entitiesToSlow.add(entityId);
        }

        public static void scheduleSpeedModifierRemove(int entityId) {
            entitiesToUnslow.add(entityId);
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            // Обрабатываем все уровни
            for (var serverLevel : event.getServer().getAllLevels()) {
                if (serverLevel instanceof ServerLevel) {
                    // Применяем замедление
                    for (Integer entityId : new HashSet<>(entitiesToSlow)) {
                        var entity = serverLevel.getEntity(entityId);
                        if (entity instanceof LivingEntity livingEntity) {
                            applySpeedModifier(livingEntity);
                        }
                        entitiesToSlow.remove(entityId);
                    }

                    // Убираем замедление
                    for (Integer entityId : new HashSet<>(entitiesToUnslow)) {
                        var entity = serverLevel.getEntity(entityId);
                        if (entity instanceof LivingEntity livingEntity) {
                            removeSpeedModifier(livingEntity);
                        }
                        entitiesToUnslow.remove(entityId);
                    }
                }
            }
        }

        private static void applySpeedModifier(LivingEntity entity) {
            var movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementSpeed != null) {
                // Сначала удаляем старый модификатор, если есть
                movementSpeed.removeModifier(SLOW_UUID);
                // Добавляем новый: -70% скорости (остается 30% = замедление в ~3.33 раза)
                movementSpeed.addTransientModifier(new AttributeModifier(
                        SLOW_UUID,
                        "milk_fog_slow",
                        -0.7,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }
        }

        private static void removeSpeedModifier(LivingEntity entity) {
            var movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementSpeed != null) {
                movementSpeed.removeModifier(SLOW_UUID);
            }
        }
    }
}