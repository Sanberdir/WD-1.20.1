package ru.imaginaerum.wd.server.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import ru.imaginaerum.wd.common.blocks.custom.AppleLeavesStages;
import net.minecraft.util.RandomSource;
import ru.imaginaerum.wd.common.items.ItemsWD;
import vectorwing.farmersdelight.common.registry.ModItems;

public class HitAppleStarBall {

    /**
     * Обрабатывает попадание StarBall в блок AppleLeavesStages.
     * Если стадия == MAX (5), сбрасывает её в 0 и спавнит яблоки и ягоды.
     * @param level серверный мир
     * @param pos позиция блока
     * @return true, если блок был AppleLeavesStages и обработан, иначе false
     */
    public static boolean handle(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AppleLeavesStages && state.getValue(AppleLeavesStages.STAGE) == 5) {
            // Сброс стадии до 0
            level.setBlock(pos, state.setValue(AppleLeavesStages.STAGE, 0), 2);

            RandomSource random = level.getRandom();
            // 45% шанс на 1-2 яблока
            if (random.nextFloat() < 0.35F) {
                int count = 1 + random.nextInt(2);
                for (int i = 0; i < count; i++) {
                    ItemEntity drop = new ItemEntity(level,
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,
                            pos.getZ() + 0.5,
                            new ItemStack(ModItems.APPLE_PIE_SLICE.get()));
                    level.addFreshEntity(drop);
                }
            }
            if (random.nextFloat() < 0.15F) {
                int count = 1 + random.nextInt(2);
                for (int i = 0; i < count; i++) {
                    ItemEntity drop = new ItemEntity(level,
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,
                            pos.getZ() + 0.5,
                            new ItemStack(ItemsWD.SPARKLING_POLLEN.get()));
                    level.addFreshEntity(drop);
                }
            }
            level.addFreshEntity(new ExperienceOrb(level,
                    pos.getX() + 0.5,
                    pos.getY() + 1.0,
                    pos.getZ() + 0.5,
                    10));
            // 100% шанс на 1-4 сладких ягод
            int berries = 1 + random.nextInt(2);
            for (int i = 0; i < berries; i++) {
                ItemEntity drop = new ItemEntity(level,
                        pos.getX() + 0.5,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5,
                        new ItemStack(ItemsWD.APPLE_JAM.get()));
                level.addFreshEntity(drop);
            }
            return true;
        }
        return false;
    }
}
