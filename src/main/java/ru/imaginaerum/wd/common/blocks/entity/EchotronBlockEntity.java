package ru.imaginaerum.wd.common.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.*;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.AABB;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.custom.EchotronBlock;

import ru.imaginaerum.wd.common.blocks.custom.MagicCompost;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import javax.annotation.Nullable;
import java.util.List;

public class EchotronBlockEntity extends BlockEntity
        implements GameEventListener.Holder<VibrationSystem.Listener>, VibrationSystem, GeoAnimatable {

    private VibrationSystem.Data vibrationData;
    private final VibrationSystem.Listener vibrationListener;
    private final VibrationSystem.User vibrationUser;

    public EchotronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ECHOTRON_ENTITY.get(), pos, state);
        this.vibrationUser = new EchotronUser(pos);
        this.vibrationData = new VibrationSystem.Data();
        this.vibrationListener = new VibrationSystem.Listener(this);
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return this.vibrationUser;
    }

    @Override
    public VibrationSystem.Listener getListener() {
        return this.vibrationListener;
    }
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        return 0;
    }


    // === Кастомный слушатель ===
    protected class EchotronUser implements VibrationSystem.User {
        private final BlockPos blockPos;
        private final PositionSource positionSource;

        public EchotronUser(BlockPos pos) {
            this.blockPos = pos;
            this.positionSource = new BlockPositionSource(pos);
        }

        @Override
        public int getListenerRadius() {
            return 8; // радиус слышимости
        }

        @Override
        public PositionSource getPositionSource() {
            return this.positionSource;
        }

        @Override
        public boolean canTriggerAvoidVibration() {
            return true;
        }

        @Override
        public boolean canReceiveVibration(ServerLevel level, BlockPos pos, GameEvent event, @Nullable GameEvent.Context context) {
            return true; // можно фильтровать по типу события
        }

        @Override
        public void onReceiveVibration(ServerLevel level, BlockPos pos, GameEvent event,
                                       @Nullable Entity entity, @Nullable Entity source, float distance) {
            BlockState state = getBlockState();
            int stage = state.getValue(EchotronBlock.STAGE);

            if (stage < 29) {
                level.setBlock(getBlockPos(),
                        state.setValue(EchotronBlock.STAGE, stage + 1), 3);
                stage++; // чтобы звук играл с обновлённым значением
            }
            if (stage == 29) {
                // НАНОСИМ УРОН SONIC BOOM И СБРАСЫВАЕМ СТАДИЮ
                float damageAmount = 10.0F; // Урон как у вардена :cite[1]

                // Определяем направление "вперёд" на основе rotation блока
                BlockState blockState = getBlockState();
                Direction direction = blockState.getValue(EchotronBlock.FACING); // Предполагаем, что у блока есть свойство FACING

                // Создаем DamageSource с null источником (так как блок не является Entity)
                DamageSource damageSource = level.damageSources().sonicBoom(null);

                // Определяем область поражения в виде "луча" вперёд от блока
                BlockPos blockPos = getBlockPos();
                AABB area = createForwardCone(blockPos, direction, 15.0F, 30.0F); // Конус длиной 15 блоков с углом 30 градусов

                // Ищем все живые сущности в области поражения
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);

                for (LivingEntity target : entities) {
                    // Проверяем, что цель не в творческом режиме и уязвима
                    if (!target.isInvulnerable() && target.isAffectedByPotions()) {
                        // Наносим урон sonic boom
                        target.hurt(damageSource, damageAmount);

                        // Добавляем эффект отбрасывания в направлении от блока
                        double dX = target.getX() - blockPos.getX();
                        double dZ = target.getZ() - blockPos.getZ();
                        target.knockback(1.0F, dX, dZ);
                    }
                }
                if (level.isNight() && !level.isRaining()) {
                    for (BlockPos posInArea : BlockPos.betweenClosed(
                            (int) area.minX, (int) area.minY, (int) area.minZ,
                            (int) area.maxX, (int) area.maxY, (int) area.maxZ
                    )) {
                        BlockState checkState = level.getBlockState(posInArea);

                        if (checkState.getBlock() instanceof MagicCompost) {
                            int currentStage = MagicCompost.getStage(checkState);

                            if (!MagicCompost.isMaxStage(checkState)) {
                                BlockState newState = MagicCompost.setStage(checkState, currentStage + 1);
                                level.setBlock(posInArea, newState, 3);

                                // Эффектные частицы
                                level.sendParticles(
                                        ParticleTypes.COMPOSTER,
                                        posInArea.getX() + 0.5,
                                        posInArea.getY() + 0.5,
                                        posInArea.getZ() + 0.5,
                                        8, 0.3, 0.3, 0.3, 0.05
                                );
                            }
                        }
                        if (checkState.getBlock() instanceof MagicCompost) {
                            int currentStage = MagicCompost.getStage(checkState);

                            if (currentStage >= 4) {
                                // === превращаем в MagicSoil ===
                                level.setBlock(posInArea, BlocksWD.MAGIC_SOIL.get().defaultBlockState(), 3);

                                // красивые частицы
                                level.sendParticles(
                                        ParticleTypes.HAPPY_VILLAGER,
                                        posInArea.getX() + 0.5,
                                        posInArea.getY() + 0.5,
                                        posInArea.getZ() + 0.5,
                                        10, 0.4, 0.4, 0.4, 0.05
                                );
                            } else {
                                // если ещё не достиг 3 стадии – повышаем
                                BlockState newState = MagicCompost.setStage(checkState, currentStage + 1);
                                level.setBlock(posInArea, newState, 3);

                                level.sendParticles(
                                        ParticleTypes.COMPOSTER,
                                        posInArea.getX() + 0.5,
                                        posInArea.getY() + 0.5,
                                        posInArea.getZ() + 0.5,
                                        8, 0.3, 0.3, 0.3, 0.05
                                );
                            }
                        }
                    }
                }
                // Сбрасываем стадию на 0
                level.setBlock(getBlockPos(),
                        state.setValue(EchotronBlock.STAGE, 0), 3);

                // Визуальные эффекты
                level.playSound(null, getBlockPos(),
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.0F);

                // Спавним частицы в направлении атаки
                spawnDirectionalParticles(level, blockPos, direction);

                return; // Прерываем выполнение, так как стадия сброшена
            }

            // Обычный звук для стадий 0-28
            float pitch = 0.5F + (stage / 29.0F);
            level.playSound(
                    null,
                    pos,
                    SoundEvents.SCULK_CLICKING,
                    SoundSource.BLOCKS,
                    1.0F,
                    pitch
            );
        }

        // Метод для создания конической области поражения впереди блока
        private AABB createForwardCone(BlockPos blockPos, Direction direction, float length, float angleDegrees) {
            double x = blockPos.getX() + 0.5;
            double y = blockPos.getY() + 0.5;
            double z = blockPos.getZ() + 0.5;

            // Определяем центр области впереди блока
            double forwardX = x + direction.getStepX() * length / 2;
            double forwardY = y + direction.getStepY() * length / 2;
            double forwardZ = z + direction.getStepZ() * length / 2;

            // Рассчитываем радиус области на основе угла
            double radius = length * Math.tan(Math.toRadians(angleDegrees / 2));

            // Создаем AABB для конической области
            return new AABB(
                    forwardX - radius, forwardY - radius, forwardZ - radius,
                    forwardX + radius, forwardY + radius, forwardZ + radius
            );
        }

        // Метод для создания направленных частиц
        private void spawnDirectionalParticles(ServerLevel level, BlockPos blockPos, Direction direction) {
            double startX = blockPos.getX() + 0.5;
            double startY = blockPos.getY() + 1.0;
            double startZ = blockPos.getZ() + 0.5;

            // Длина луча частиц
            double length = 15.0;

            // Создаем частицы вдоль луча в направлении атаки
            for (double distance = 1.0; distance <= length; distance += 0.5) {
                // Позиция вдоль луча
                double particleX = startX + direction.getStepX() * distance;
                double particleY = startY + direction.getStepY() * distance;
                double particleZ = startZ + direction.getStepZ() * distance;

                // Добавляем небольшой случайный разброс
                double spread = 0.3;
                double offsetX = level.random.nextGaussian() * spread;
                double offsetY = level.random.nextGaussian() * spread;
                double offsetZ = level.random.nextGaussian() * spread;

                // Спавним частицы sonic boom вдоль луча :cite[1]
                level.sendParticles(ParticleTypes.SONIC_BOOM,
                        particleX + offsetX,
                        particleY + offsetY,
                        particleZ + offsetZ,
                        0, // count = 0 для одной частицы
                        0.0, 0.0, 0.0, // нулевая скорость
                        0.1);
            }

            // Дополнительные частицы для визуального эффекта
            for (int i = 0; i < 10; i++) {
                double distance = length * (0.2 + 0.8 * level.random.nextDouble());
                double particleX = startX + direction.getStepX() * distance;
                double particleY = startY + direction.getStepY() * distance;
                double particleZ = startZ + direction.getStepZ() * distance;

                level.sendParticles(ParticleTypes.SONIC_BOOM,
                        particleX, particleY, particleZ,
                        1,
                        level.random.nextGaussian() * 0.1,
                        level.random.nextGaussian() * 0.1,
                        level.random.nextGaussian() * 0.1,
                        0.05);
            }
        }

        @Override
        public void onDataChanged() {
            EchotronBlockEntity.this.setChanged();
        }

        @Override
        public boolean requiresAdjacentChunksToBeTicking() {
            return true;
        }
    }
}


