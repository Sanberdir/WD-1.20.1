package ru.imaginaerum.wd.common.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.custom.*;
import ru.imaginaerum.wd.common.blocks.registry_blocks_plaints.MagicSoilFarmlandData;
import ru.imaginaerum.wd.common.blocks.registry_blocks_plaints.PepperRegistry;

import java.util.*;

@Mod.EventBusSubscriber(modid = WD.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventBusEvents {

    private static long lastDayTime = -1;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;

    // === СИСТЕМА ФЕРМЫ ===
    @SubscribeEvent
    public static void onLevelTickFarmland(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;

        long timeOfDay = event.level.getDayTime() % 24000;
        if (lastDayTime > timeOfDay && event.level instanceof ServerLevel serverLevel) {
            dryAllSoil(serverLevel);
            upgradeStagePepper(serverLevel);
        }
        lastDayTime = timeOfDay;
    }
    @SubscribeEvent
    public static void onLivingDropsFlameArrow(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();

        if (!entity.getPersistentData().getBoolean("FlameArrowKill")) return;
        entity.getPersistentData().remove("FlameArrowKill");

        for (ItemEntity itemEntity : event.getDrops()) {
            ItemStack stack = itemEntity.getItem();
            ItemStack smelted = getSmeltedResult(entity.level(), stack);
            if (!smelted.isEmpty()) {
                smelted.setCount(stack.getCount());
                itemEntity.setItem(smelted);
            }
        }
    }

    private static ItemStack getSmeltedResult(Level level, ItemStack input) {
        if (level instanceof ServerLevel serverLevel) {
            var recipeManager = serverLevel.getRecipeManager();
            var container = new net.minecraft.world.SimpleContainer(input);
            return recipeManager
                    .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, container, serverLevel)
                    .map(r -> r.assemble(container, serverLevel.registryAccess()))
                    .orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }
    @SubscribeEvent
    public static void tickMoistSoil(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.isRaining()) continue;
            MagicSoilFarmlandData data = MagicSoilFarmlandData.get(level);

            var iterator = data.getFarmlands().iterator();
            while (iterator.hasNext()) {
                BlockPos pos = iterator.next();
                if (!level.hasChunkAt(pos)) continue;

                BlockState state = level.getBlockState(pos);
                if (state.is(BlocksWD.MAGIC_SOIL_FARMLAND.get()) && !state.getValue(MagicSoilFarmland.MOIST)) {
                    level.setBlock(pos, state.setValue(MagicSoilFarmland.MOIST, true), 2);
                }
            }
        }
    }

    private static void dryAllSoil(ServerLevel level) {
        MagicSoilFarmlandData data = MagicSoilFarmlandData.get(level);
        var iterator = data.getFarmlands().iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!level.hasChunkAt(pos)) continue;

            BlockState state = level.getBlockState(pos);
            if (state.is(BlocksWD.MAGIC_SOIL_FARMLAND.get())) {
                if (state.getValue(MagicSoilFarmland.MOIST)) {
                    level.setBlock(pos, state.setValue(MagicSoilFarmland.MOIST, false), 3);
                } else {
                    level.setBlock(pos, BlocksWD.MAGIC_SOIL.get().defaultBlockState(), 3);
                    iterator.remove();
                }
            } else {
                iterator.remove();
            }
        }
    }

    private static void upgradeStagePepper(ServerLevel level) {
        PepperRegistry data = PepperRegistry.get(level);
        var iterator = data.getPepperBlocks().iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!level.hasChunkAt(pos)) continue;

            BlockState state = level.getBlockState(pos);
            if (state.is(BlocksWD.BRIGHT_PEPPER_SEEDS.get())) {
                int currentStage = state.getValue(BrightPepperSeeds.STAGE);
                final int MAX_STAGE = 12;
                if (currentStage < MAX_STAGE) {
                    level.setBlock(pos, state.setValue(BrightPepperSeeds.STAGE, currentStage + 1), 2);
                }
            } else {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        DragolitBlock.onEntityDeath(event.getEntity(), event.getSource());
        DragolitGrid.onEntityDeath(event.getEntity(), event.getSource());
        DragoliteCage.onEntityDeath(event.getEntity(), event.getSource());
    }
}