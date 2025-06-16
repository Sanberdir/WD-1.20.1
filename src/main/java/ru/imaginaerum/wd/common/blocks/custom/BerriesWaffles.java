package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.wd.common.items.ItemsWD;

public class BerriesWaffles extends Block {
    public static final IntegerProperty WAFFLERS = IntegerProperty.create("wafflers", 0, 7);
    private final RegistryObject<Item> waffleItem;

    public BerriesWaffles(Properties properties, RegistryObject<Item> waffleItem) {
        super(properties);
        this.waffleItem = waffleItem;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WAFFLERS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WAFFLERS);
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
            return blockBelow.getValue(WAFFLERS) == 7;
        }
        if (blockBelow.isFaceSturdy(world, position.below(), Direction.UP)) {
            return true;
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        int stage = state.getValue(WAFFLERS);
        double height = 2.0D + stage * 2.0D;
        return Block.box(3.0D, 0.0D, 3.0D, 13.0D, height, 13.0D);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack itemInHand = player.getItemInHand(hand);
        int currentStage = state.getValue(WAFFLERS);

        if (player.isShiftKeyDown()) {
            if (currentStage > 0) {
                level.setBlock(pos, state.setValue(WAFFLERS, currentStage - 1), 3);

                ItemStack itemToGive = new ItemStack(waffleItem.get());
                if (!player.addItem(itemToGive)) {
                    player.drop(itemToGive, false);
                }
                level.playSound(player, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

                player.swing(hand);
                return InteractionResult.SUCCESS;
            } else {
                level.removeBlock(pos, false);
                level.playSound(player, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                ItemStack itemToGive = new ItemStack(waffleItem.get());
                if (!player.addItem(itemToGive)) {
                    player.drop(itemToGive, false);
                }
                return InteractionResult.SUCCESS;
            }
        } else {
            if (!itemInHand.isEmpty() && itemInHand.is(waffleItem.get())) {
                if (currentStage < 7) {
                    level.setBlock(pos, state.setValue(WAFFLERS, currentStage + 1), 3);
                    level.playSound(player, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

                    if (!player.isCreative()) {
                        itemInHand.shrink(1);
                    }
                    player.swing(hand);
                    return InteractionResult.SUCCESS;
                }
            } else if (itemInHand.isEmpty()) {
                // Если рука пуста, восстанавливаем насыщение и сытость
                if (currentStage > 0) {
                    level.setBlock(pos, state.setValue(WAFFLERS, currentStage - 1), 3); // Уменьшаем стадию
                } else if (currentStage == 0) {
                    level.removeBlock(pos, false); // Если стадия равна 0, удаляем блок
                }

                // Общая логика для всех случаев
                player.getFoodData().eat(6, 1.0F); // Восстанавливаем 6 единиц насыщения и 1 единицу сытости
                level.playSound(player, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
