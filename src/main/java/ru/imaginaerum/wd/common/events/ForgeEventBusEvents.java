package ru.imaginaerum.wd.common.events;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.registry_blocks_plaints.MagicSoilFarmlandData;
import ru.imaginaerum.wd.common.blocks.registry_blocks_plaints.PepperRegistry;
import ru.imaginaerum.wd.common.blocks.custom.MagicSoilFarmland;
import ru.imaginaerum.wd.common.blocks.custom.PepperSeeds;

import java.util.HashSet;

@Mod.EventBusSubscriber(modid = WD.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventBusEvents {
    @SubscribeEvent
    public static void onLevelTickFarmland(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide) {
            long timeOfDay = event.level.getDayTime() % 24000;
            if (timeOfDay == 0) {
                dryAllSoil((ServerLevel) event.level);
                upgradeStagePepper((ServerLevel) event.level);
            }
        }
    }
    private static void upgradeStagePepper(ServerLevel level) {
        PepperRegistry data = PepperRegistry.get(level);

        for (BlockPos pos : new HashSet<>(data.getPepperBlocks())) {
            if (!level.hasChunkAt(pos)) continue;

            BlockState state = level.getBlockState(pos);

            // Если блок всё ещё перец — пробуем увеличить стадию
            if (state.is(BlocksWD.PEPPER_SEEDS.get())) {
                int currentStage = state.getValue(PepperSeeds.STAGE);
                final int MAX_STAGE = 12;

                if (currentStage < MAX_STAGE) {
                    int nextStage = currentStage + 1;
                    BlockState newState = state.setValue(PepperSeeds.STAGE, nextStage);

                    // Флаг 2 — только обновляем клиент без соседних апдейтов
                    level.setBlock(pos, newState, 2);
                }
            } else {
                // Если блока уже нет, убираем из SavedData
                data.remove(pos);
            }
        }
    }


    private static void dryAllSoil(ServerLevel level) {
        MagicSoilFarmlandData data = MagicSoilFarmlandData.get(level);

        // создаём копию, чтобы безопасно удалять элементы
        for (BlockPos pos : new HashSet<>(data.getFarmlands())) {
            if (!level.hasChunkAt(pos)) continue;

            BlockState state = level.getBlockState(pos);

            // проверяем, что это наш MagicSoilFarmland
            if (state.is(BlocksWD.MAGIC_SOIL_FARMLAND.get())) {
                if (state.getValue(MagicSoilFarmland.MOIST)) {
                    // сушим
                    level.setBlock(pos, state.setValue(MagicSoilFarmland.MOIST, false), 3);
                } else {
                    // превращаем в обычную землю
                    level.setBlock(pos, BlocksWD.MAGIC_SOIL.get().defaultBlockState(), 3);
                    data.remove(pos); // удаляем из сохранённого списка
                }
            } else {
                // если блок уже не farmland, убираем из списка
                data.remove(pos);
            }
        }
    }

    @SubscribeEvent
    public static void tickMoistSoil(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.isRaining()) continue;

            MagicSoilFarmlandData data = MagicSoilFarmlandData.get(level);
            for (BlockPos pos : new HashSet<>(data.getFarmlands())) {
                if (!level.hasChunkAt(pos)) continue;

                BlockState state = level.getBlockState(pos);
                if (state.is(BlocksWD.MAGIC_SOIL_FARMLAND.get())
                        && !state.getValue(MagicSoilFarmland.MOIST)) {
                    level.setBlock(pos, state.setValue(MagicSoilFarmland.MOIST, true), 2);
                }
            }
        }
    }
}