package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.items.ItemsWD;

import java.util.List;

public class MarinadedPot extends FacingBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

    public MarinadedPot(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STAGE, 0));
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.getItem() == BlocksWD.MARINADED_POT_FROM_MEAT_GOAT.get().asItem()) {
            tooltip.add(Component.translatable("tooltip.wd.pot_from_meat_goat")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STAGE);
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

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        int currentStage = state.getValue(STAGE);

        // Проверяем, что игрок использует палку и блок ещё не исчерпал все стадии
        if (itemStack.is(Items.STICK) && currentStage < 3) {
            if (!level.isClientSide) {
                // Убираем одну палку
                if (!player.isCreative()) {
                    itemStack.shrink(1);
                }

                // Увеличиваем стадию
                int newStage = currentStage + 1;
                level.setBlock(pos, state.setValue(STAGE, newStage), 3);

                // Даём шашлык из козлятины игроку
                ItemStack kebabStack = new ItemStack(ItemsWD.GOAT_MEAT_KEBAB.get());
                if (!player.getInventory().add(kebabStack)) {
                    // Если инвентарь полный, выпадаем предмет на землю
                    player.drop(kebabStack, false);
                }

                // Проигрываем звук подбирания предмета
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);

                // Если достигли последней стадии, превращаем в цветочный горшок
                if (newStage >= 3) {
                    level.setBlock(pos, BlocksWD.POT.get().defaultBlockState(), 3);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.use(state, level, pos, player, hand, hit);
    }
}