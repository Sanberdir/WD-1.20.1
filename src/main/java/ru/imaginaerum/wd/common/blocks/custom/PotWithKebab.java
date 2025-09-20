package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.wd.common.blocks.BlocksWD;

import java.util.List;

public class PotWithKebab extends FacingBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public PotWithKebab(Properties properties) {
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
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.getItem() == BlocksWD.POT_FROM_MEAT_GOAT.get().asItem()) {
            tooltip.add(Component.translatable("tooltip.wd.pot_from_meat_goat")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && state.is(BlocksWD.POT_FROM_MEAT_GOAT.get())) {
            level.scheduleTick(pos, this, 14400);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.is(BlocksWD.POT_FROM_MEAT_GOAT.get())) {
            level.setBlock(pos, BlocksWD.MARINADED_POT_FROM_MEAT_GOAT.get()
                    .defaultBlockState()
                    .setValue(FACING, state.getValue(FACING)), 3);
        }
    }

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

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.is(BlocksWD.POT_FROM_MEAT_GOAT.get())) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.ENTITY_EFFECT, x, y, z, 0.9, 0.2, 0.4); // бордовый
        }
    }
}
