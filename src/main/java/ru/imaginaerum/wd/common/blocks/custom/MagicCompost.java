package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class MagicCompost extends Block {
    // Создаем свойство для стадий (0, 1, 3)
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

    public MagicCompost(Properties pProperties) {
        super(pProperties);
        // Устанавливаем начальное состояние (стадия 0)
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(STAGE);
    }

    // Вспомогательные методы для работы со стадиями
    public static int getStage(BlockState state) {
        return state.getValue(STAGE);
    }

    public static BlockState setStage(BlockState state, int stage) {
        return state.setValue(STAGE, Math.min(Math.max(stage, 0), 3));
    }

    public static BlockState nextStage(BlockState state) {
        int currentStage = getStage(state);
        return setStage(state, currentStage + 1);
    }

    public static BlockState previousStage(BlockState state) {
        int currentStage = getStage(state);
        return setStage(state, currentStage - 1);
    }

    public static boolean isMaxStage(BlockState state) {
        return getStage(state) >= 3;
    }

    public static boolean isMinStage(BlockState state) {
        return getStage(state) <= 0;
    }
}