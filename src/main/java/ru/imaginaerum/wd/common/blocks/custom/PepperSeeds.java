package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.PepperRegistry;

public class PepperSeeds extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 12);

    public PepperSeeds(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }
    private static final VoxelShape[] SHAPES = new VoxelShape[13];

    static {
        for (int i = 0; i <= 12; i++) {
            // Высота от 1/16 блока до 16/16 блока (1.0)
            double height = (i + 1) * 1.0 / 16.0 * 16.0; // Minecraft использует единицы 0-16
            SHAPES[i] = Block.box(4.0, 0.0, 4.0, 12.0, height, 12.0);
        }
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(STAGE)];
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(STAGE);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            PepperRegistry.registerPepper(pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            System.out.println("🗑️ Pepper removed at: " + pos);
            PepperRegistry.unregisterPepper(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);

        if (!level.isClientSide) {
            if (!canSurvive(state, level, pos)) {
                // Разрушаем блок — выбросит дропы и вызовет onRemove -> удаление из реестра
                level.destroyBlock(pos, true);
            }
        }
    }
    @Override
    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        // Перец выживает только если блок снизу — именно MAGIC_SOIL_FARMLAND
        BlockState below = pLevel.getBlockState(pPos.below());
        return below.is(BlocksWD.MAGIC_SOIL_FARMLAND.get());
    }
}