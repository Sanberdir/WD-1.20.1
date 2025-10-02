package ru.imaginaerum.wd.common.items.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import ru.imaginaerum.wd.common.blocks.BlocksWD;

public class DirtBoneMeal extends Item {
    private final Block targetBlock;

    public DirtBoneMeal(Properties properties, Block targetBlock) {
        super(properties);
        this.targetBlock = targetBlock; // Указываем целевой блок (Warped или Crimson Nylium)
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // Проверяем, что блок, на который щелкнули, это Незерак
        if (world.getBlockState(pos).getBlock() == Blocks.DIRT) {
            BlockPos abovePos = pos.above();

            // Проверяем, что блок над Незераком — это воздух
            if (world.getBlockState(abovePos).getBlock() == Blocks.AIR || world.getBlockState(abovePos).getBlock() == Blocks.CAVE_AIR) {
                // Заменяем Незерак на целевой блок
                world.setBlock(pos, targetBlock.defaultBlockState(), 3);

                stack.shrink(1);
                player.swing(context.getHand());

                // Воспроизводим звук использования костной муки
                world.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

                // Отправляем анимацию частиц (эффект костной муки)
                if (world instanceof ServerLevel) {
                    ((ServerLevel) world).sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.0);
                }

                return InteractionResult.sidedSuccess(world.isClientSide());
            }
        }
        if (world.getBlockState(pos).getBlock() == BlocksWD.MAGIC_SOIL.get()) {
            BlockPos abovePos = pos.above();

            // Проверяем, что блок над Незераком — это воздух
            if (world.getBlockState(abovePos).getBlock() == Blocks.AIR || world.getBlockState(abovePos).getBlock() == Blocks.CAVE_AIR) {
                // Заменяем Незерак на целевой блок
                world.setBlock(pos, targetBlock.defaultBlockState(), 3);

                stack.shrink(1);
                player.swing(context.getHand());

                // Воспроизводим звук использования костной муки
                world.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

                // Отправляем анимацию частиц (эффект костной муки)
                if (world instanceof ServerLevel) {
                    ((ServerLevel) world).sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.0);
                }

                return InteractionResult.sidedSuccess(world.isClientSide());
            }
        }
        // Возвращаем результат отказа, если преобразование не произошло
        return InteractionResult.PASS;
    }
}

