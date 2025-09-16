package ru.imaginaerum.wd.common.entities.goals_entity_mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.custom.RottenPie;
import ru.imaginaerum.wd.common.blocks.custom.RottenPieCage;

import java.util.EnumSet;

public class ZombieEatPieGoal extends MoveToBlockGoal {
    private final Zombie zombie;

    private int eatCooldown = 0;
    private int breakProgress = 0;
    private static final int BREAK_THRESHOLD = 40; // ~2 секунды при 20 тик/сек

    public ZombieEatPieGoal(Zombie zombie, double speed, int searchRange) {
        super(zombie, speed, searchRange, 6);
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return super.canUse() && zombie.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    @Override
    public void tick() {
        super.tick();

        if (eatCooldown > 0) {
            eatCooldown--;
            return;
        }

        BlockPos pos = this.blockPos;
        Level level = zombie.level();

        // расстояние только по X/Z, чтобы зомби мог стоять на торте
        double dx = zombie.getX() - (pos.getX() + 0.5);
        double dz = zombie.getZ() - (pos.getZ() + 0.5);
        double distanceSqXZ = dx * dx + dz * dz;

        if (!this.isReachedTarget() && distanceSqXZ >= 2.0D) {
            return;
        }

        BlockState state = level.getBlockState(pos);

        // --- Ломаем клетку постепенно ---
        if (state.getBlock() instanceof RottenPieCage) {
            breakProgress++;

            if (breakProgress % 10 == 0) {
                level.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE,
                        0.5F, 0.8F + level.random.nextFloat() * 0.2F);
            }

            if (breakProgress >= BREAK_THRESHOLD) {
                // ставим обычный пирог на месте клетки
                level.setBlock(pos, BlocksWD.ROTTEN_PIE.get().defaultBlockState().setValue(RottenPie.STAGE, 0), 3);
                level.playSound(null, pos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE,
                        1.0F, 0.9F + level.random.nextFloat() * 0.2F);

                zombie.getNavigation().stop();
                eatCooldown = 10; // небольшая пауза
                breakProgress = 0;

                return; // следующий тик зомби увидит пирог и съест
            }

            return; // продолжаем ломать клетку
        }

        // --- Поедаем пирог ---
        if (state.getBlock() instanceof RottenPie) {
            int stage = state.getValue(RottenPie.STAGE);
            if (stage < 3) {
                level.setBlock(pos, state.setValue(RottenPie.STAGE, stage + 1), 3);
            } else {
                level.removeBlock(pos, false);
            }

            level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.HOSTILE,
                    0.7F, 0.9F + level.random.nextFloat() * 0.2F);

            if (level.random.nextFloat() < 0.7F) {
                zombie.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
            }

            zombie.getNavigation().stop();
            eatCooldown = 35;
        }
    }

    @Override
    protected boolean isValidTarget(LevelReader levelReader, BlockPos blockPos) {
        BlockState state = levelReader.getBlockState(blockPos);
        return state.getBlock() instanceof RottenPie || state.getBlock() instanceof RottenPieCage;
    }
}
