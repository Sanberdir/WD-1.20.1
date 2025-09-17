package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.registry_blocks_plaints.PepperRegistry;
import ru.imaginaerum.wd.common.items.ItemsWD;

public class BrightPepperSeeds extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 12);

    private static final VoxelShape[] SHAPES = new VoxelShape[13];

    static {
        for (int i = 0; i <= 12; i++) {
            // Высота от 1/16 блока до полного блока
            double height = (i + 1) * (16.0 / 13.0); // равномерный рост до 16
            SHAPES[i] = Block.box(4.0, 0.0, 4.0, 12.0, Math.min(16.0, height), 12.0);
        }
    }

    public BrightPepperSeeds(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    // ==============================
    // ======= Взаимодействие =======
    // ==============================
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && state.getValue(STAGE) == 12) {
            harvestPepper(level, pos, state);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void harvestPepper(Level level, BlockPos pos, BlockState state) {
        RandomSource random = level.random;
        int dropCount;

        // 30% шанс на 3–4 перца, иначе 1–2
        if (random.nextFloat() < 0.3f) {
            dropCount = random.nextBoolean() ? 3 : 4;
        } else {
            dropCount = random.nextBoolean() ? 1 : 2;
        }

        popResource(level, pos, new ItemStack(ItemsWD.BRIGHT_PEPPER.get(), dropCount));

        // Сбрасываем стадию до 9
        level.setBlock(pos, state.setValue(STAGE, 9), 3);
    }

    // ==============================
    // ======= Геометрия блока ======
    // ==============================
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(STAGE)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    // ==============================
    // ======= Жизненный цикл =======
    // ==============================
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            PepperRegistry.get((net.minecraft.server.level.ServerLevel) level).add(pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            PepperRegistry.get((net.minecraft.server.level.ServerLevel) level).remove(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);

        if (!level.isClientSide && !canSurvive(state, level, pos)) {
            // уничтожаем блок с дропом → вызовет onRemove → удалит из PepperData
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(BlocksWD.MAGIC_SOIL_FARMLAND.get());
    }
}
