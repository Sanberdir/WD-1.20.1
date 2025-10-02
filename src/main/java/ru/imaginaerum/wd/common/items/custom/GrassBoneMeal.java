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

public class GrassBoneMeal extends Item {

    public GrassBoneMeal(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // Проверяем, что блок, на который щелкнули, это земля
        if (world.getBlockState(pos).getBlock() == Blocks.DIRT) {
            BlockPos abovePos = pos.above();

            // Проверяем, что блок над землёй — это воздух
            if (world.getBlockState(abovePos).getBlock() == Blocks.AIR || world.getBlockState(abovePos).getBlock() == Blocks.CAVE_AIR) {
                // Заменяем землю на дёрн
                world.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);

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

        // Проверяем, что блок, на который щелкнули, это MAGIC_SOIL
        if (world.getBlockState(pos).getBlock() == BlocksWD.MAGIC_SOIL.get()) {
            BlockPos abovePos = pos.above();

            // Проверяем, что блок над MAGIC_SOIL — это воздух
            if (world.getBlockState(abovePos).getBlock() == Blocks.AIR || world.getBlockState(abovePos).getBlock() == Blocks.CAVE_AIR) {
                // Заменяем MAGIC_SOIL на MAGIC_SOIL_GRASS
                world.setBlock(pos, BlocksWD.MAGIC_SOIL_GRASS.get().defaultBlockState(), 3);

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