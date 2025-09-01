package ru.imaginaerum.wd.common.events;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.MagicSoilFarmlandRegistry;
import ru.imaginaerum.wd.common.blocks.PepperRegistry;
import ru.imaginaerum.wd.common.blocks.custom.MagicSoilFarmland;
import ru.imaginaerum.wd.common.blocks.custom.PepperSeeds;

import java.util.HashSet;
import java.util.Set;

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
        Set<BlockPos> pepperPositions = PepperRegistry.getPepperBlocksCopy();

        for (BlockPos pos : pepperPositions) {
            // Пропускаем незагруженные чанки
            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);

            // Если блок всё ещё перец — пробуем увеличить стадию.
            if (state.getBlock() instanceof PepperSeeds) {
                int currentStage = state.getValue(PepperSeeds.STAGE);
                final int MAX_STAGE = 12;

                if (currentStage < MAX_STAGE) {
                    int nextStage = currentStage + 1;
                    BlockState newState = state.setValue(PepperSeeds.STAGE, nextStage);

                    // Используем флаг 2 — не трогаем соседние апдейты, только обновляем клиент.
                    level.setBlock(pos, newState, 2);

                    // Проверка состояния после установки (без логов)
                    BlockState after = level.getBlockState(pos);
                    if (!(after.getBlock() instanceof PepperSeeds)) {
                        BlockState below = level.getBlockState(pos.below());
                        // intentionally no logging; onRemove будет обработан самим блоком
                    }
                }
            }
        }
    }


    private static void dryAllSoil(ServerLevel level) {
        for (BlockPos pos : new HashSet<>(MagicSoilFarmlandRegistry.FARMLANDS)) {
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof MagicSoilFarmland farmland) {
                if (state.getValue(MagicSoilFarmland.MOIST)) {
                    // если был влажный → сушим
                    level.setBlock(pos, state.setValue(MagicSoilFarmland.MOIST, false), 3);
                } else {
                    // если уже сухой → превращаем в MagicSoil
                    level.setBlock(pos, BlocksWD.MAGIC_SOIL.get().defaultBlockState(), 3);
                    MagicSoilFarmlandRegistry.FARMLANDS.remove(pos); // больше не farmland
                }
            }
        }
    }

}