package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.wd.common.items.ItemsWD;

public class GlowingJamBlock extends AbstractSkullBlock {
    // Хитбоксы для банки

    private static final VoxelShape SHAPES_1_1 = Block.box(5.0D, 7.0D, 5.0D, 11.0D, 8.0D, 11.0D);
    private static final VoxelShape SHAPES_1_2 = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 6.0D, 12.0D);
    private static final VoxelShape SHAPES_1 = Shapes.or(SHAPES_1_1, SHAPES_1_2);

    public GlowingJamBlock(GlowingJamBlock.Type pType, Properties pProperties) {
        super(pType, pProperties);
    }
    public static enum Types implements GlowingJamBlock.Type {
        GLOWING_JAM;

        private Types() {
        }
    }
    public interface Type extends SkullBlock.Type {
    }
    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            destroyAbove(world, pos);
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    private void destroyAbove(Level world, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState aboveState = world.getBlockState(abovePos);

        if (aboveState.getBlock() instanceof BerriesWaffles) {
            world.destroyBlock(abovePos, true);
            destroyAbove(world, abovePos);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public BlockState updateShape(BlockState currentState, Direction direction, BlockState adjacentState, LevelAccessor world, BlockPos currentPos, BlockPos adjacentPos) {
        if (!currentState.canSurvive(world, currentPos)) {
            world.scheduleTick(currentPos, this, 1);
        }
        return super.updateShape(currentState, direction, adjacentState, world, currentPos, adjacentPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos position) {
        BlockState blockBelow = world.getBlockState(position.below());
        if (blockBelow.is(this)) {
            return true;
        }
        if (blockBelow.isFaceSturdy(world, position.below(), Direction.UP)) {
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack itemInHand = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // Убираем банку (даём предмет игроку)
            level.removeBlock(pos, false);
            level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);

            ItemStack itemToGive = new ItemStack(ItemsWD.GLOWING_JAM.get());
            if (!player.addItem(itemToGive)) {
                player.drop(itemToGive, false);
            }

            player.swing(hand);
            return InteractionResult.SUCCESS;
        } else {
            if (!itemInHand.isEmpty() && itemInHand.is(ItemsWD.GLOWING_JAM.get())) {
                // Если игрок держит банку джема, ничего не делаем (блок уже есть)
                return InteractionResult.PASS;
            } else if (itemInHand.isEmpty()) {
                // Если рука пуста, съедаем джем
                level.removeBlock(pos, false);
                player.getFoodData().eat(14, 0.5F);
                RandomSource random = level.getRandom();
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 220, 0));

                level.playSound(player, pos, SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext collisionContext) {
        return SHAPES_1;
    }
}