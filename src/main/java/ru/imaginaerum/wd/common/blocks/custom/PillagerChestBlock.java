package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PillagerChestBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Хитбоксы для каждого направления
    private static final VoxelShape SHAPE_NORTH = Block.box(2.0D, 0.0D, 4.0D, 14.0D, 8.0D, 12.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2.0D, 0.0D, 4.0D, 14.0D, 8.0D, 12.0D);
    private static final VoxelShape SHAPE_EAST = Block.box(4.0D, 0.0D, 2.0D, 12.0D, 8.0D, 14.0D);
    private static final VoxelShape SHAPE_WEST = Block.box(4.0D, 0.0D, 2.0D, 12.0D, 8.0D, 14.0D);

    public PillagerChestBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        // зеркалим через поворот, привязанный к mirror
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext collisionContext) {
        Direction direction = state.getValue(FACING);

        switch (direction) {
            case SOUTH:
                return SHAPE_SOUTH;
            case EAST:
                return SHAPE_EAST;
            case WEST:
                return SHAPE_WEST;
            case NORTH:
            default:
                return SHAPE_NORTH;
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);

        return belowState.isFaceSturdy(level, belowPos, Direction.UP, net.minecraft.world.level.block.SupportType.FULL);
    }
}