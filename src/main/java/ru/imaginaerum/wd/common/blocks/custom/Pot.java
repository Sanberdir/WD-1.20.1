package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Pot extends FacingBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public Pot(Properties properties) {
        super(properties);
    }
    // VoxelShape для каждого направления
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            box(5.0, 0.0, 5.0, 11.0, 7.0, 11.0),
            box(7.0, 7.0, 6.0, 9.0, 9.0, 10.0)
    );

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            box(5.0, 0.0, 5.0, 11.0, 7.0, 11.0),
            box(6.0, 7.0, 7.0, 10.0, 9.0, 9.0)
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
            box(5.0, 0.0, 5.0, 11.0, 7.0, 11.0),
            box(7.0, 7.0, 6.0, 9.0, 9.0, 10.0)
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            box(5.0, 0.0, 5.0, 11.0, 7.0, 11.0),
            box(6.0, 7.0, 7.0, 10.0, 9.0, 9.0)
    );


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);

        switch (direction) {
            case EAST:
                return SHAPE_EAST;
            case SOUTH:
                return SHAPE_SOUTH;
            case WEST:
                return SHAPE_WEST;
            default: // NORTH
                return SHAPE_NORTH;
        }
    }

}
