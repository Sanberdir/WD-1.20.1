package ru.imaginaerum.wd.server.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import ru.imaginaerum.wd.common.sounds.CustomSoundEvents;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Random;


@Mod.EventBusSubscriber
public class OpenKingPillagersChest {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Level level = player.level();
            BlockPos pos = event.getPos();
            BlockState state = level.getBlockState(pos);
            Block clickedBlock = state.getBlock();

            // Проверяем, что кликнули по королевскому сундуку
            if (clickedBlock == BlocksWD.GOLDEN_CHEST_KING_PILLAGER.get()) {
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();

                // Проверяем наличие ключа в любой из рук
                boolean hasKeyInMain = mainHand.getItem() == ItemsWD.THE_KING_PILLAGERS_KEY.get();
                boolean hasKeyInOff = offHand.getItem() == ItemsWD.THE_KING_PILLAGERS_KEY.get();

                if (hasKeyInMain || hasKeyInOff) {
                    // Уменьшаем ключ в соответствующей руке
                    if (hasKeyInMain) {
                        mainHand.shrink(1);
                    } else {
                        offHand.shrink(1);
                    }

                    level.destroyBlock(pos, false);
                    event.setCanceled(true); // Отменяем стандартное действие
                    level.playSound(null,pos, CustomSoundEvents.OPEN_CHESTS.get(), SoundSource.BLOCKS,1,1);
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

    private static void dropItem(Level level, BlockPos pos, ItemStack itemStack) {
        if (!level.isClientSide && level != null && pos != null) {
            // Центрируем позицию дропа над сундуком
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
    }

    private static JsonObject loadConfig() {
        JsonObject mergedConfig = new JsonObject();
        try {
            ResourceLocation folderPath = new ResourceLocation("wd", "loot_tables/king_pillager_chest");
            var resourceManager = ServerLifecycleHooks.getCurrentServer().getResourceManager();
            Map<ResourceLocation, Resource> resources = resourceManager.listResources(folderPath.getPath(),
                    path -> path.getPath().endsWith(".json"));

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (InputStream inputStream = entry.getValue().open()) {
                    JsonObject fileConfig = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
                    fileConfig.entrySet().forEach(entryFile -> mergedConfig.add(entryFile.getKey(), entryFile.getValue()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mergedConfig;
    }
}