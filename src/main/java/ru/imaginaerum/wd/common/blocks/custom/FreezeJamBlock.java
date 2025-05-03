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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.wd.common.effects.EffectsWD;

public class FreezeJamBlock extends Block {
    public static final IntegerProperty JAM_NUMBER = IntegerProperty.create("jam_number", 0, 7);
    // Хитбоксы для каждого направления
    private static final VoxelShape SHAPES_1 = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 8.0D, 11.0D);
    private static final VoxelShape SHAPES_SMALL_UP = Block.box(5.0D, 9.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final VoxelShape SHAPES_2 = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D);
    private static final VoxelShape SHAPES_3_4 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    private static final VoxelShape SHAPES_5 = Shapes.or(SHAPES_SMALL_UP, SHAPES_3_4);
    private static final VoxelShape SHAPES_6 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private final RegistryObject<Item> jamItem;
    public FreezeJamBlock(Properties pProperties, RegistryObject<Item> jamItem) {
        super(pProperties);
        this.jamItem = jamItem;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(JAM_NUMBER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(JAM_NUMBER);
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
        int currentStage = state.getValue(JAM_NUMBER);

        if (player.isShiftKeyDown()) {
            if (currentStage > 0) {
                level.setBlock(pos, state.setValue(JAM_NUMBER, currentStage - 1), 3);

                ItemStack itemToGive = new ItemStack(jamItem.get());
                if (!player.addItem(itemToGive)) {
                    player.drop(itemToGive, false);
                }
                level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);

                player.swing(hand);
                return InteractionResult.SUCCESS;
            } else {
                level.removeBlock(pos, false);
                level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
                ItemStack itemToGive = new ItemStack(jamItem.get());
                if (!player.addItem(itemToGive)) {
                    player.drop(itemToGive, false);
                }
                return InteractionResult.SUCCESS;
            }
        } else {
            if (!itemInHand.isEmpty() && itemInHand.is(jamItem.get())) {
                if (currentStage < 7) {
                    level.setBlock(pos, state.setValue(JAM_NUMBER, currentStage + 1), 3);
                    level.playSound(player, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

                    if (!player.isCreative()) {
                        itemInHand.shrink(1);
                    }
                    player.swing(hand);
                    return InteractionResult.SUCCESS;
                }
            } else if (itemInHand.isEmpty()) {
                // Если рука пуста, восстанавливаем насыщение и сытость
                if (currentStage > 0) {
                    level.setBlock(pos, state.setValue(JAM_NUMBER, currentStage - 1), 3); // Уменьшаем стадию
                } else if (currentStage == 0) {
                    level.removeBlock(pos, false); // Если стадия равна 0, удаляем блок
                }

                // Общая логика для всех случаев
                player.getFoodData().eat(10, 0.3F);

                RandomSource random = level.getRandom();
                    player.addEffect(new MobEffectInstance(EffectsWD.FREEZE.get(), 2200, 0));

                if (random.nextFloat() < 0.9F) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
                }

                level.playSound(player, pos, SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);

                player.swing(hand);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext collisionContext) {
        int jamNumber = state.getValue(JAM_NUMBER);
        switch (jamNumber) {
            case 0:
                return SHAPES_1;
            case 1:
                return SHAPES_2;
            case 2:
            case 3:
                return SHAPES_3_4;
            case 4:
                return SHAPES_5;
            case 5:
            case 6:
            case 7:
                return SHAPES_6;
            default:
                return SHAPES_1;
        }
    }
}
