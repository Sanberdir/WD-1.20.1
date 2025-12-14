package ru.imaginaerum.wd.common.events;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
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
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = WD.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventBusEvents {

    private static long lastDayTime = -1;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;

    // === ОБРАБОТКА ВХОДА И СИНХРОНИЗАЦИИ ===
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        try {
            // ПРЕДВАРИТЕЛЬНАЯ ЗАГРУЗКА ВСЕХ ГЛАВ
            TaskManager.preloadAllChapters(serverPlayer.server);

            // Авто-разблокировка корневых нодов
            var serverNodes = ProgressionServerLoader.loadNodes(serverPlayer.server);
            for (ProgressNode n : serverNodes) {
                if ((n.getParentId() == null || n.getParentId().isEmpty()) && !n.isLocked()) {
                    ProgressionUnlockManager.unlock(serverPlayer, n.getId());
                }
            }
        } catch (Throwable t) {
            System.err.println("[ArsMelima] Failed to auto-unlock roots on login: " + t.getMessage());
        }

        // Полная синхронизация
        syncAllData(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        syncAllData(serverPlayer);
    }

    private static void syncAllData(ServerPlayer player) {
        // Синхронизация XP
        int xp = CookingXPManager.getXp(player);
        int level = CookingXPManager.getLevel(player);
        NetworkCookingXp.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncCookingXpPacket(xp, level));

        // Синхронизация прогрессии
        NetworkCookingXp.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncUnlockedProgressPacket(ProgressionUnlockManager.getUnlockedList(player)));

        // Синхронизация задач
        syncAllTaskProgress(player);
    }

    private static void syncAllTaskProgress(ServerPlayer player) {
        Map<String, Map<String, Integer>> allProgress = new HashMap<>();
        MinecraftServer server = player.getServer();

        if (server != null) {
            // Синхронизируем все загруженные главы
            for (String chapterId : TaskManager.getLoadedChapterIds()) {
                List<Task> tasks = TaskManager.getTasksForChapter(server, chapterId);
                Map<String, Integer> chapterProgress = new HashMap<>();

                for (Task task : tasks) {
                    // Инициализируем главу если нужно
                    ServerTaskStorage.initializeChapter(player, chapterId);

                    // Используем метод с указанием главы
                    int progress = ServerTaskStorage.getProgress(player, chapterId, task.getId());
                    chapterProgress.put(task.getId(), progress);
                }

                if (!chapterProgress.isEmpty()) {
                    allProgress.put(chapterId, chapterProgress);
                }
            }
        }

        NetworkCookingXp.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncAllTaskProgressPacket(allProgress)
        );
    }

    // === ОБРАБОТКА КРАФТА И ПЛАВКИ ===
    // === ОБРАБОТКА КРАФТА И ПЛАВКИ ===
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();

        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack result = event.getCrafting();
            if (!result.isEmpty()) {
                handleCrafting(serverPlayer, result, result.getCount(), "crafting");
            }
        }

        // Исправленный вызов XP - передаем player и itemStack
        if (player != null && !player.level().isClientSide) {
            addCraftingXp(player, event.getCrafting());
        }
    }

    // Обработка обычной плавки в печи
    @SubscribeEvent
    public static void onFurnaceSmelting(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ItemStack result = event.getSmelting();
            if (!result.isEmpty()) {
                handleCrafting(serverPlayer, result, result.getCount(), "smelting");
                addCraftingXp(serverPlayer, result); // ДОБАВЛЕНО: XP за плавку
            }
        }
    }

    // Обработка копчения в коптильне
    @SubscribeEvent
    public static void onSmokerSmoking(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ItemStack result = event.getSmelting();
            if (!result.isEmpty() && isSmokingRecipe(result, event.getEntity().level())) {
                handleCrafting(serverPlayer, result, result.getCount(), "smoking");
                addCraftingXp(serverPlayer, result); // ДОБАВЛЕНО: XP за копчение
            }
        }
    }

    // Обработка приготовления на костре
    @SubscribeEvent
    public static void onCampfireCookingResult(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState blockState = level.getBlockState(pos);

        if (blockState.getBlock() == Blocks.CAMPFIRE || blockState.getBlock() == Blocks.SOUL_CAMPFIRE) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CampfireBlockEntity campfire) {
                // Проверяем все слоты костра на наличие готовых предметов
                for (int i = 0; i < campfire.getItems().size(); i++) {
                    ItemStack itemInSlot = campfire.getItems().get(i);

                    // Если в слоте есть предмет, проверяем может ли он приготовиться
                    if (!itemInSlot.isEmpty()) {
                        ItemStack cookingResult = getCampfireCookingResult(level, itemInSlot);
                        if (!cookingResult.isEmpty()) {
                            // Предмет может приготовиться на костре
                            handleCrafting((ServerPlayer) event.getEntity(), cookingResult, 1, "campfire_cooking");
                            addCraftingXp((ServerPlayer) event.getEntity(), cookingResult); // ДОБАВЛЕНО: XP за костер
                        }
                    }
                }
            }
        }
    }

    // Обработка извлечения готовых предметов из костра
    @SubscribeEvent
    public static void onItemTakenFromCampfire(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState blockState = level.getBlockState(pos);

        if (blockState.getBlock() == Blocks.CAMPFIRE || blockState.getBlock() == Blocks.SOUL_CAMPFIRE) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CampfireBlockEntity campfire) {
                // Используем доступные методы для проверки готовности
                for (int i = 0; i < campfire.getItems().size(); i++) {
                    ItemStack itemInSlot = campfire.getItems().get(i);
                    if (!itemInSlot.isEmpty()) {
                        // Проверяем готовность через доступные данные
                        ItemStack result = getCampfireCookingResult(level, itemInSlot);
                        if (!result.isEmpty()) {
                            // Если предмет может быть приготовлен, добавляем прогресс
                            handleCrafting((ServerPlayer) event.getEntity(), result, 1, "campfire_cooking");
                            addCraftingXp((ServerPlayer) event.getEntity(), result);
                            break;
                        }
                    }
                }
            }
        }
    }

    // Обработка извлечения предметов из печи/коптильни
    @SubscribeEvent
    public static void onItemTakenFromFurnace(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState blockState = level.getBlockState(pos);

        // Проверяем все типы печей
        if (blockState.getBlock() instanceof AbstractFurnaceBlock) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
                ItemStack result = furnace.getItem(2); // Слот результата
                if (!result.isEmpty()) {
                    // Определяем тип рецепта
                    String recipeType = "smelting";
                    if (blockState.getBlock() == Blocks.SMOKER) {
                        recipeType = "smoking";
                    } else if (blockState.getBlock() == Blocks.BLAST_FURNACE) {
                        recipeType = "blasting";
                    }

                    handleCrafting((ServerPlayer) event.getEntity(), result, result.getCount(), recipeType);
                    addCraftingXp((ServerPlayer) event.getEntity(), result);
                }
            }
        }
    }

    /**
     * Получает результат приготовления предмета на костре
     */
    private static ItemStack getCampfireCookingResult(Level level, ItemStack input) {
        RecipeManager recipeManager = level.getRecipeManager();
        var recipes = recipeManager.getRecipesFor(RecipeType.CAMPFIRE_COOKING, new SimpleContainer(input), level);

        if (!recipes.isEmpty()) {
            // Берем первый подходящий рецепт
            return recipes.get(0).getResultItem(level.registryAccess()).copy();
        }

        return ItemStack.EMPTY;
    }

    // Вспомогательные методы проверки рецептов
    private static boolean isSmokingRecipe(ItemStack result, Level level) {
        RecipeManager recipeManager = level.getRecipeManager();
        var recipes = recipeManager.getRecipesFor(RecipeType.SMOKING, new SimpleContainer(result), level);
        return !recipes.isEmpty();
    }

    private static boolean isCampfireRecipe(ItemStack result, Level level) {
        RecipeManager recipeManager = level.getRecipeManager();
        var recipes = recipeManager.getRecipesFor(RecipeType.CAMPFIRE_COOKING, new SimpleContainer(result), level);
        return !recipes.isEmpty();
    }



    /**
     * Получает результат приготовления предмета на костре
     */


    // ОБНОВИТЕ метод onCampfireCooking для обработки готовых предметов
    @SubscribeEvent
    public static void onCampfireCooking(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ItemStack result = event.getSmelting();
            if (!result.isEmpty()) {
                // Проверяем, был ли это рецепт костра
                if (isCampfireRecipe(result, event.getEntity().level())) {
                    handleCrafting(serverPlayer, result, result.getCount(), "campfire_cooking");
                }
            }
        }
    }

    private static void handleCrafting(ServerPlayer player, ItemStack result, int count, String recipeType) {
        String itemId = ForgeRegistries.ITEMS.getKey(result.getItem()).toString();

        // ПРЕЖДЕ чем искать задачи, проверяем доступные (разблокированные) главы
        List<String> unlockedChapters = getUnlockedChaptersForPlayer(player);

        // Ищем задачи ТОЛЬКО в разблокированных главах
        List<Task> tasks = TaskManager.getTasksByItemWithForceLoad(player.getServer(), itemId);

        // ФИЛЬТРУЕМ: оставляем только задачи из разблокированных глав
        List<Task> unlockedTasks = tasks.stream()
                .filter(task -> unlockedChapters.contains(task.getChapterId()))
                .collect(Collectors.toList());

        for (Task task : unlockedTasks) {
            if (task.matchesRecipeType(recipeType)) {
                processTask(player, task, count, recipeType);
            }
        }
    }

    private static List<String> getUnlockedChaptersForPlayer(ServerPlayer player) {
        List<String> unlockedChapters = new ArrayList<>();
        MinecraftServer server = player.getServer();

        if (server == null) return unlockedChapters;

        // Динамически получаем все доступные главы
        List<String> allChapters = TaskManager.discoverChapterIds(server.getResourceManager());

        // Первая глава всегда разблокирована
        if (!allChapters.isEmpty()) {
            unlockedChapters.add(allChapters.get(0));
        }

        // Проверяем выполнение предыдущих глав для разблокировки последующих
        for (int i = 0; i < allChapters.size() - 1; i++) {
            String currentChapter = allChapters.get(i);
            String nextChapter = allChapters.get(i + 1);

            if (isLearningChapterCompletedOnServer(player, currentChapter)) {
                unlockedChapters.add(nextChapter);
            } else {
                break; // Если глава не завершена, останавливаем разблокировку
            }
        }

        return unlockedChapters;
    }

    private static void processTask(ServerPlayer player, Task task, int count, String recipeType) {
        String chapterId = task.getChapterId();

        // Глава гарантированно разблокирована на этом этапе
        ServerTaskStorage.initializeChapter(player, chapterId);
        int currentProgress = ServerTaskStorage.getProgress(player, chapterId, task.getId());
        int requiredCount = task.getRequiredCount();
        int maxPossibleIncrement = requiredCount - currentProgress;
        int actualIncrement = Math.min(count, maxPossibleIncrement);

        if (actualIncrement > 0) {
            int newProgress = ServerTaskStorage.incrementProgress(player, chapterId, task.getId(), actualIncrement);
            syncProgressToClient(player, task, newProgress, chapterId);

            // Проверяем не завершилась ли глава и не нужно ли разблокировать следующую
            checkChapterCompletion(player, chapterId);
        }
    }

    private static void checkChapterCompletion(ServerPlayer player, String completedChapterId) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        // Динамически определяем следующую главу
        List<String> allChapters = TaskManager.discoverChapterIds(server.getResourceManager());
        int completedIndex = allChapters.indexOf(completedChapterId);

        if (completedIndex >= 0 && completedIndex < allChapters.size() - 1) {
            String nextChapterId = allChapters.get(completedIndex + 1);

            // Проверяем действительно ли глава завершена
            if (isLearningChapterCompletedOnServer(player, completedChapterId)) {
                // Автоматически разблокируем следующую главу
                // (если у вас есть система разблокировки через ProgressionUnlockManager)
                System.out.println("[ArsMelima] Chapter '" + completedChapterId + "' completed, unlocking '" + nextChapterId + "'");

                // Если нужно разблокировать через вашу систему прогрессии:
                // ProgressionUnlockManager.unlock(player, nextChapterId);
            }
        }
    }

    private static boolean isLearningChapterCompletedOnServer(ServerPlayer player, String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) return false;

        List<Task> tasks = TaskManager.getTasksForChapter(player.getServer(), chapterId);
        if (tasks == null || tasks.isEmpty()) {
            return true; // Если нет задач - считаем выполненной
        }

        for (Task t : tasks) {
            int progress = ServerTaskStorage.getProgress(player, chapterId, t.getId());
            if (progress < t.getRequiredCount()) {
                return false;
            }
        }

        return true;
    }

    private static void syncProgressToClient(ServerPlayer player, Task task, int progress, String chapterId) {
        NetworkCookingXp.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncTaskProgressPacket(chapterId, task.getId(), progress)
        );
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===
    private static void addCraftingXp(Player player, ItemStack crafting) {
        if (player == null || crafting.isEmpty()) return;

        if (crafting.isEdible()) {
            CookingXPManager.addXp(player, crafting.getCount() * 5);
        }

        Item item = crafting.getItem();
        if (item == Items.CAKE || item == ItemsWD.WIZARD_PIE.get() || item == ItemsWD.ROTTEN_PIE.get()) {
            CookingXPManager.addXp(player, crafting.getCount() * 25);
        }
    }

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

    // === ОБРАБОТКА СМЕРТИ ===
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