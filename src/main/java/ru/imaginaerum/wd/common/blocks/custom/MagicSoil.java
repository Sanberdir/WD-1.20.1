package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ru.imaginaerum.wd.common.blocks.BlocksWD;

public class MagicSoil extends Block {
    public MagicSoil(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.getItemInHand(hand).is(ItemTags.HOES)) {
            level.setBlock(pos, BlocksWD.MAGIC_SOIL_FARMLAND.get().defaultBlockState(), 3);
            level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1,1);
            if (!level.isClientSide) {
                player.getItemInHand(hand).hurtAndBreak(
                        1,
                        player,
                        (p) -> p.broadcastBreakEvent(hand)
                );
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
