package ru.imaginaerum.wd.common.events;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.client.gui.ars_melima.NetworkCookingXp;
import ru.imaginaerum.wd.client.gui.ars_melima.SyncCookingXpPacket;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.CookingXPManager;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.custom.*;
import ru.imaginaerum.wd.common.blocks.registry_blocks_plaints.MagicSoilFarmlandData;
import ru.imaginaerum.wd.common.blocks.registry_blocks_plaints.PepperRegistry;

import java.util.HashSet;

@Mod.EventBusSubscriber(modid = WD.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventBusEvents {
    private static long lastDayTime = -1;
    @SubscribeEvent
    public static void onLevelTickFarmland(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide) {
            long timeOfDay = event.level.getDayTime() % 24000;

            if (lastDayTime > timeOfDay) {
                // Время "обернулось" через 0 тиков
                dryAllSoil((ServerLevel) event.level);
                upgradeStagePepper((ServerLevel) event.level);
            }

            lastDayTime = timeOfDay;
        }
    }
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        CookingXPManager.resetXp(serverPlayer);

        NetworkCookingXp.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new SyncCookingXpPacket(0, 0)
        );

    }
    @SubscribeEvent
    public static void onXpCookedCampfire(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        var level = event.getLevel();
        var pos = event.getPos();
        var blockState = level.getBlockState(pos);
        var block = blockState.getBlock();
        if (block != Blocks.CAMPFIRE && block != Blocks.SOUL_CAMPFIRE) return;

        // Проверяем заполненность костра
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CampfireBlockEntity campfire) {
            int filledSlots = 0;
            for (ItemStack item : campfire.getItems()) {
                if (!item.isEmpty()) filledSlots++;
            }
            if (filledSlots >= 4) return;
        }

        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (stack.isEmpty()) return;

        // Проверяем есть ли рецепт для этого предмета в костре
        if (hasCampfireRecipe(level, stack)) {
            CookingXPManager.addXp(event.getEntity(), 5);
        }
    }
    private static boolean hasCampfireRecipe(Level level, ItemStack stack) {
        // Получаем менеджер рецептов
        RecipeManager recipeManager = level.getRecipeManager();

        // Ищем рецепты костра для этого предмета
        var recipes = recipeManager.getRecipesFor(
                RecipeType.CAMPFIRE_COOKING,
                new SimpleContainer(stack),
                level
        );

        return !recipes.isEmpty();
    }
    @SubscribeEvent
    public static void onItemSmelted(net.minecraftforge.event.entity.player.PlayerEvent.ItemSmeltedEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide) return;

        ItemStack smelted = event.getSmelting();

        // Добавляем опыт за каждую приготовленную еду
        if (smelted.isEdible()) {
            CookingXPManager.addXp(player, 5); // например, 10 XP за приготовление еды
        }
    }
    @SubscribeEvent
    public static void onPlayerLoginXpCooking(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        // Получаем актуальные данные игрока с сервера
        int xp = CookingXPManager.getXp(serverPlayer);
        int level = CookingXPManager.getLevel(serverPlayer);

        // Отправляем на клиент
        NetworkCookingXp.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new SyncCookingXpPacket(xp, level)
        );
    }

    private static void upgradeStagePepper(ServerLevel level) {
        PepperRegistry data = PepperRegistry.get(level);

        for (BlockPos pos : new HashSet<>(data.getPepperBlocks())) {
            if (!level.hasChunkAt(pos)) continue;

            BlockState state = level.getBlockState(pos);

            // Если блок всё ещё перец — пробуем увеличить стадию
            if (state.is(BlocksWD.BRIGHT_PEPPER_SEEDS.get())) {
                int currentStage = state.getValue(BrightPepperSeeds.STAGE);
                final int MAX_STAGE = 12;

                if (currentStage < MAX_STAGE) {
                    int nextStage = currentStage + 1;
                    BlockState newState = state.setValue(BrightPepperSeeds.STAGE, nextStage);

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
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        DragolitBlock.onEntityDeath(event.getEntity(), event.getSource());
        DragolitGrid.onEntityDeath(event.getEntity(), event.getSource());
        DragoliteCage.onEntityDeath(event.getEntity(), event.getSource());
    }
}