package ru.imaginaerum.wd.common.items.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.custom.MagicSoilFarmland;

import javax.annotation.Nullable;
import java.util.List;

public class WateringCan extends Item {

    private static final int MAX_CAPACITY = 1000;
    private static final int WATER_USAGE = 50;

    public WateringCan(Properties properties) {
        super(properties);
    }

    public static boolean isFull(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getBoolean("water_full");
    }

    public static void setFull(ItemStack stack, boolean full) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("water_full", full);
    }

    public static int getWaterAmount(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt("water_amount");
    }

    public static void setWaterAmount(ItemStack stack, int amount) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("water_amount", Math.min(amount, MAX_CAPACITY));

        // Обновляем визуальное состояние для модели
        setFull(stack, amount > 0);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int waterAmount = getWaterAmount(stack);
        tooltip.add(Component.literal(waterAmount + "/" + MAX_CAPACITY + " mb"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        } else {
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                var pos = hitResult.getBlockPos();
                if (!level.mayInteract(player, pos)) {
                    return InteractionResultHolder.pass(stack);
                }

                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    int currentWater = getWaterAmount(stack);

                    // Если лейка не полная, доливаем воду
                    if (currentWater < MAX_CAPACITY) {
                        // Наполняем полностью
                        setWaterAmount(stack, MAX_CAPACITY);
                        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                    }
                }
            }
            return InteractionResultHolder.pass(stack);
        }
    }
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK; // Покачивание руки, похожее на полив
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // Проверяем, что блок - это магическая farmland и в лейке достаточно воды
        if (level.getBlockState(pos).is(BlocksWD.MAGIC_SOIL_FARMLAND.get()) && getWaterAmount(stack) >= WATER_USAGE) {
            BlockState state = level.getBlockState(pos);

            // Проверяем, что блок не увлажнен (MOIST == false)
            if (state.hasProperty(MagicSoilFarmland.MOIST) && !state.getValue(MagicSoilFarmland.MOIST)) {
                level.setBlock(pos, state.setValue(MagicSoilFarmland.MOIST, true), 3);

                int currentWater = getWaterAmount(stack);
                setWaterAmount(stack, currentWater - WATER_USAGE);

                // Звук
                level.playSound(player, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);

                // Частицы воды
                for (int i = 0; i < 10; i++) {
                    double offsetX = 0.5 + (level.random.nextDouble() - 0.5);
                    double offsetY = 1.0;
                    double offsetZ = 0.5 + (level.random.nextDouble() - 0.5);
                    level.addParticle(ParticleTypes.SPLASH, pos.getX() + offsetX, pos.getY() + offsetY, pos.getZ() + offsetZ,
                            0.0, 0.1, 0.0);
                }

                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onCraftedBy(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player) {
        super.onCraftedBy(stack, level, player);
        if (!stack.hasTag()) {
            setWaterAmount(stack, 0);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        int waterAmount = getWaterAmount(stack);
        return waterAmount > 0 && waterAmount < MAX_CAPACITY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int waterAmount = getWaterAmount(stack);
        return Math.round((float) waterAmount / MAX_CAPACITY * 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3366CC; // Синий цвет для индикатора воды
    }
}