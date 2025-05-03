package ru.imaginaerum.wd.server.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.items.ItemsWD;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Random;


@Mod.EventBusSubscriber
public class OpenPillagersChest {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Level level = player.level();
            BlockPos pos = event.getPos();
            BlockState state = level.getBlockState(pos);
            Block clickedBlock = state.getBlock();

            // Проверяем, что кликнули по королевскому сундуку
            if (clickedBlock == BlocksWD.THE_PILLAGERS_CHEST.get()) {
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();

                // Проверяем наличие ключа в любой из рук
                boolean hasKeyInMain = mainHand.getItem() == ItemsWD.THE_PILLAGERS_KEY.get();
                boolean hasKeyInOff = offHand.getItem() == ItemsWD.THE_PILLAGERS_KEY.get();

                if (hasKeyInMain || hasKeyInOff) {
                    // Уменьшаем ключ в соответствующей руке
                    if (hasKeyInMain) {
                        mainHand.shrink(1);
                    } else {
                        offHand.shrink(1);
                    }
                    level.destroyBlock(pos, false);
                    event.setCanceled(true); // Отменяем стандартное действие

                    JsonObject config = loadConfig();
                    JsonElement itemsConfig = config.get("items");
                    Random rand = new Random();

                    if (itemsConfig != null && itemsConfig.isJsonArray()) {
                        for (JsonElement itemElement : itemsConfig.getAsJsonArray()) {
                            JsonObject itemData = itemElement.getAsJsonObject();
                            if (itemData.has("min") && itemData.has("max") && itemData.has("chance")) {
                                int minAmount = itemData.get("min").getAsInt();
                                int maxAmount = itemData.get("max").getAsInt();
                                int chance = itemData.get("chance").getAsInt();

                                if (rand.nextInt(100) < chance) {
                                    int amount = rand.nextInt(maxAmount - minAmount + 1) + minAmount;
                                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemData.get("item").getAsString()));
                                    if (item != null) {
                                        ItemStack itemStack = new ItemStack(item, amount);
                                        // Дроп предмета над сундуком
                                        dropItem(level, pos.above(), itemStack);
                                    }
                                }
                            } else {
                                System.err.println("Ошибка конфигурации: " + itemData);
                            }
                        }
                    }
                }
            }
        }
    }

    // Метод для дропа предмета
    private static void dropItem(Level level, BlockPos position, ItemStack itemStack) {
        if (!level.isClientSide && level != null && position != null) {
            ItemEntity itemEntity = new ItemEntity(level, position.getX(), position.getY(), position.getZ(), itemStack);
            level.addFreshEntity(itemEntity);
        }
    }

    // Метод для загрузки конфигурации из JSON файлов
    private static JsonObject loadConfig() {
        JsonObject mergedConfig = new JsonObject();

        try {
            // Указываем путь к папке с JSON-файлами
            ResourceLocation folderPath = new ResourceLocation("wd", "loot_tables/pillager_chest");
            var resourceManager = ServerLifecycleHooks.getCurrentServer().getResourceManager();

            // Получаем ресурсы в папке pillagers
            Map<ResourceLocation, Resource> resources = resourceManager.listResources(folderPath.getPath(), path -> path.getPath().endsWith(".json"));

            // Проходим по всем ресурсам
            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (InputStream inputStream = entry.getValue().open()) {
                    InputStreamReader reader = new InputStreamReader(inputStream);
                    JsonObject fileConfig = JsonParser.parseReader(reader).getAsJsonObject();

                    // Объединяем содержимое файлов
                    fileConfig.entrySet().forEach(entryFile -> mergedConfig.add(entryFile.getKey(), entryFile.getValue()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();  // Логирование ошибки
        }

        return mergedConfig;
    }
}