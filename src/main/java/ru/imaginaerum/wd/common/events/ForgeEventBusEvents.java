package ru.imaginaerum.wd.common.events;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraftforge.registries.ForgeRegistries;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.client.gui.ars_melima.*;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.CookingXPManager;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ProgressionServerLoader;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ProgressionUnlockManager;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.SyncUnlockedProgressPacket;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.custom.*;
import ru.imaginaerum.wd.common.blocks.registry_blocks_plaints.MagicSoilFarmlandData;
import ru.imaginaerum.wd.common.blocks.registry_blocks_plaints.PepperRegistry;
import ru.imaginaerum.wd.common.items.ItemsWD;

import java.util.*;

@Mod.EventBusSubscriber(modid = WD.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventBusEvents {

    private static long lastDayTime = -1;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // раз в ~1 секунду

    // --- Авто-открытие root-нодов и синхронизация XP ---
    @SubscribeEvent
    public static void onPlayerLoginXpUnlock(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        try {
            var serverNodes = ProgressionServerLoader.loadNodes(serverPlayer.server);
            for (ProgressNode n : serverNodes) {
                if ((n.getParentId() == null || n.getParentId().isEmpty()) && !n.isLocked()) {
                    ProgressionUnlockManager.unlock(serverPlayer, n.getId());
                }
            }
        } catch (Throwable t) {
            System.err.println("[ArsMelima] Failed to auto-unlock roots on login: " + t.getMessage());
        }

        syncXpAndProgress(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        syncXpAndProgress(serverPlayer);
    }

    private static void syncXpAndProgress(ServerPlayer player) {
        int xp = CookingXPManager.getXp(player);
        int level = CookingXPManager.getLevel(player);

        NetworkCookingXp.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncCookingXpPacket(xp, level));

        NetworkCookingXp.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncUnlockedProgressPacket(ProgressionUnlockManager.getUnlockedList(player)));
    }

    // --- Работа с фермой и перцами ---
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
    // В класс ForgeEventBusEvents добавить:
    @SubscribeEvent
    public static void onPlayerLoginTasks(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        // Принудительно загружаем задачи для нужной главы
        String chapterId = "cutting_techniques";
        List<Task> tasks = TaskManager.getTasksForChapter(serverPlayer.getServer(), chapterId);
        System.out.println("[ArsMelima] Preloaded " + tasks.size() + " tasks for chapter: " + chapterId);

        // Синхронизируем прогресс
        syncAllTaskProgress(serverPlayer);
    }

    private static void syncAllTaskProgress(ServerPlayer player) {
        Map<String, Map<String, Integer>> allProgress = new HashMap<>();

        // Собираем прогресс по всем главам и задачам
        MinecraftServer server = player.getServer();
        if (server != null) {
            for (String chapterId : TaskManager.getLoadedChapterIds()) {
                List<Task> tasks = TaskManager.getTasksForChapter(server, chapterId);
                Map<String, Integer> chapterProgress = new HashMap<>();

                for (Task task : tasks) {
                    // Используем новую схему хранения с chapterId
                    int progress = ServerTaskStorage.getProgress(player, chapterId, task.getId());
                    chapterProgress.put(task.getId(), progress);
                }

                if (!chapterProgress.isEmpty()) {
                    allProgress.put(chapterId, chapterProgress);
                }
            }
        }

        // Отправляем на клиент
        NetworkCookingXp.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncAllTaskProgressPacket(allProgress)
        );

        System.out.println("[ArsMelima] Synced all task progress to player: " + allProgress.size() + " chapters");
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

    // --- XP за крафт и кулинарию ---
    @SubscribeEvent
    public static void onXpCookedCampfire(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;

        var level = event.getLevel();
        var pos = event.getPos();
        var blockState = level.getBlockState(pos);
        var block = blockState.getBlock();
        if (block != Blocks.CAMPFIRE && block != Blocks.SOUL_CAMPFIRE) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CampfireBlockEntity campfire) {
            int filledSlots = 0;
            for (ItemStack item : campfire.getItems()) if (!item.isEmpty()) filledSlots++;
            if (filledSlots >= 4) return;
        }

        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (stack.isEmpty()) return;
        if (hasCampfireRecipe(level, stack)) CookingXPManager.addXp(event.getEntity(), 5);
    }

    private static boolean hasCampfireRecipe(Level level, ItemStack stack) {
        RecipeManager recipeManager = level.getRecipeManager();
        var recipes = recipeManager.getRecipesFor(RecipeType.CAMPFIRE_COOKING, new SimpleContainer(stack), level);
        return !recipes.isEmpty();
    }

    @SubscribeEvent
    public static void onItemSmeltedXpCooking(PlayerEvent.ItemSmeltedEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide) return;

        ItemStack smelted = event.getSmelting();
        if (smelted.isEdible()) {
            int totalXp = smelted.getCount() * 5;
            CookingXPManager.addXp(player, totalXp);
        }
    }
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ItemStack result = event.getCrafting();
            if (!result.isEmpty()) {
                handleCrafting(serverPlayer, result, result.getCount(), "crafting");
            }
        }
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ItemStack result = event.getSmelting();
            if (!result.isEmpty()) {
                handleCrafting(serverPlayer, result, result.getCount(), "smelting");
            }
        }
    }

    // В методе handleCrafting в ForgeEventBusEvents исправить:
    private static void handleCrafting(ServerPlayer player, ItemStack result, int count, String recipeType) {
        String itemId = ForgeRegistries.ITEMS.getKey(result.getItem()).toString();
        List<Task> tasks = TaskManager.getTasksByItem(itemId);

        for (Task task : tasks) {
            if (task.getRecipeType().equals(recipeType)) {
                String learningChapterId = findLearningChapterForTask(task, player.getServer());

                if (learningChapterId != null) {
                    int currentProgress = ServerTaskStorage.getProgress(player, learningChapterId, task.getId());
                    int requiredCount = task.getRequiredCount();

                    // Если задача уже выполнена - пропускаем
                    if (currentProgress >= requiredCount) {
                        continue;
                    }

                    // Вычисляем сколько можно добавить
                    int maxPossibleIncrement = requiredCount - currentProgress;
                    int actualIncrement = Math.min(count, maxPossibleIncrement);

                    if (actualIncrement > 0) {
                        int newProgress = ServerTaskStorage.incrementProgress(
                                player, learningChapterId, task.getId(), actualIncrement
                        );

                        syncProgressToClient(player, task, newProgress, learningChapterId);

                        System.out.println("[ArsMelima] Task progress updated: " + learningChapterId + "/" + task.getId() +
                                " +" + actualIncrement + " = " + newProgress + "/" + requiredCount);

                        if (newProgress >= requiredCount) {
                            System.out.println("[ArsMelima] TASK COMPLETED: " + learningChapterId + "/" + task.getId());
                        }
                    }
                }
            }
        }
    }

    private static void syncProgressToClient(ServerPlayer player, Task task, int progress, String learningChapterId) {
        NetworkCookingXp.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncTaskProgressPacket(learningChapterId, task.getId(), progress)
        );
    }

    private static String findLearningChapterForTask(Task task, MinecraftServer server) {
        if (server == null) return null;

        for (String chapterId : TaskManager.getLoadedChapterIds()) {
            List<Task> chapterTasks = TaskManager.getTasksForChapter(server, chapterId);
            for (Task t : chapterTasks) {
                if (t.getId().equals(task.getId())) {
                    return chapterId;
                }
            }
        }
        return null;
    }
    @SubscribeEvent
    public static void onItemCraftingXpCooking(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide) return;

        ItemStack crafting = event.getCrafting();
        Item item = crafting.getItem();

        if (crafting.isEdible()) CookingXPManager.addXp(player, crafting.getCount() * 5);
        if (item == Items.CAKE || item == ItemsWD.WIZARD_PIE.get() || item == ItemsWD.ROTTEN_PIE.get())
            CookingXPManager.addXp(player, crafting.getCount() * 25);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        CookingXPManager.resetXp(serverPlayer);
        NetworkCookingXp.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncCookingXpPacket(0, 0));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        DragolitBlock.onEntityDeath(event.getEntity(), event.getSource());
        DragolitGrid.onEntityDeath(event.getEntity(), event.getSource());
        DragoliteCage.onEntityDeath(event.getEntity(), event.getSource());
    }
}
