package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public class AppleLeavesStages extends Block implements BonemealableBlock, SimpleWaterloggedBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 5);
    public static final BooleanProperty WATERLOGGED = BooleanProperty.create("waterlogged");

    // Leaf decay properties
    public static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
    public static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;
    private static final int MAX_DISTANCE = 7;

    public AppleLeavesStages(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(STAGE, 0)
                .setValue(WATERLOGGED, false)
                .setValue(DISTANCE, MAX_DISTANCE)
                .setValue(PERSISTENT, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, WATERLOGGED, DISTANCE, PERSISTENT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(STAGE, 0)
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER)
                .setValue(DISTANCE, MAX_DISTANCE)
                .setValue(PERSISTENT, true);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // Trigger ticks for both growth and decay
        return (state.getValue(DISTANCE) == MAX_DISTANCE && !state.getValue(PERSISTENT)) || state.getValue(STAGE) < 5;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel serverLevel, BlockPos pos, RandomSource randomSource) {
        // Decay logic first
        if (state.getValue(DISTANCE) == MAX_DISTANCE && !state.getValue(PERSISTENT)) {
            dropResources(state, serverLevel, pos);
            serverLevel.removeBlock(pos, false);
            return;
        }
        // Growth logic
        super.randomTick(state, serverLevel, pos, randomSource);
        if (!serverLevel.isClientSide) {
            int stage = state.getValue(STAGE);
            boolean waterlogged = state.getValue(WATERLOGGED);
            int growthChance = waterlogged ? 3 : 5;
            if (stage < 5 && randomSource.nextInt(growthChance) == 0) {
                serverLevel.setBlock(pos, state.setValue(STAGE, stage + 1), 2);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, updateDistance(state, level, pos), 3);
    }

    private static BlockState updateDistance(BlockState state, LevelAccessor level, BlockPos pos) {
        int distance = MAX_DISTANCE;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            mutable.setWithOffset(pos, dir);
            distance = Math.min(distance, getDistanceAt(level.getBlockState(mutable)) + 1);
            if (distance == 1) break;
        }
        return state.setValue(DISTANCE, distance);
    }

    private static int getDistanceAt(BlockState neighbor) {
        return getOptionalDistanceAt(neighbor).orElse(MAX_DISTANCE);
    }

    public static java.util.OptionalInt getOptionalDistanceAt(BlockState state) {
        if (state.is(BlockTags.LOGS)) {
            return java.util.OptionalInt.of(0);
        } else if (state.hasProperty(DISTANCE)) {
            return java.util.OptionalInt.of(state.getValue(DISTANCE));
        }
        return java.util.OptionalInt.empty();
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return !state.getValue(WATERLOGGED) && fluid == Fluids.WATER;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!state.getValue(WATERLOGGED) && fluidState.getType() == Fluids.WATER) {
            level.setBlock(pos, state.setValue(WATERLOGGED, true), 3);
            level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
            return true;
        }
        return false;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        // Schedule distance update for decay
        level.scheduleTick(pos, this, 1);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(Items.BONE_MEAL) && state.getValue(STAGE) < 5) {
            return InteractionResult.PASS;
        }

        if (state.getValue(STAGE) == 5) {
            if (!level.isClientSide) {
                int apples = level.random.nextInt(3) + 1;
                ItemStack applesStack = new ItemStack(Items.APPLE, apples);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);

                boolean added = player.addItem(applesStack);
                if (!added) {
                    ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, applesStack);
                    level.addFreshEntity(itemEntity);
                }
                level.setBlock(pos, state.setValue(STAGE, 0), 2);
            }
            return InteractionResult.SUCCESS;
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader levelReader, BlockPos blockPos, BlockState blockState, boolean b) {
        return blockState.getValue(STAGE) < 5;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int currentStage = state.getValue(STAGE);
        if (currentStage < 5 && random.nextInt(100) < 30) {
            level.setBlock(pos, state.setValue(STAGE, currentStage + 1), 2);
        }
    }
}