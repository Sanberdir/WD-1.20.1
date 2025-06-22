package ru.imaginaerum.wd.server.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import ru.imaginaerum.wd.common.particles.ModParticles;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class HitEntityHandler {
    private static final Map<String, JsonObject> MOB_DROPS = new HashMap<>();

    public static void handleHitEntity(EntityHitResult hitResult, ServerLevel level) {
        Entity entity = hitResult.getEntity();
        ResourceLocation entityId = EntityType.getKey(entity.getType());

        // Загружаем конфигурацию при первом попадании
        if (MOB_DROPS.isEmpty()) {
            loadMobDropsConfig();
        }

        JsonObject mobConfig = MOB_DROPS.get(entityId.toString());

        // Специальная обработка для игроков
        // Специальная обработка для игроков
        if (entity instanceof Player) {
            Player player = (Player) entity;
            FoodData foodData = player.getFoodData();

            if (foodData.getFoodLevel() >= 20) { // Если сыт (максимальный уровень сытости)
                // Наносим урон 4 единицы
                player.hurt(player.damageSources().magic(), 4.0f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 1.0f);
            } else {
                // Иначе добавляем 5 единиц сытости
                foodData.setFoodLevel(Math.min(foodData.getFoodLevel() + 5, 20));
            }

            spawnEffects(level, entity);
            return;
        }

        // Если есть конфиг - обрабатываем дропы
        if (mobConfig != null) {
            // Визуальные эффекты
            spawnEffects(level, entity);

            // Обрабатываем дроп предметов
            if (mobConfig.has("drops")) {
                processItemDrops(level, entity, mobConfig.getAsJsonObject("drops"));
            }

            // Обрабатываем дроп опыта
            if (mobConfig.has("experience")) {
                processExperienceDrops(level, entity, mobConfig.getAsJsonObject("experience"));
            }

            entity.discard();
        }
        // Если конфига нет - трансформируем моба
        else {
            transformMob(level, entity);
        }
    }

    private static void transformMob(ServerLevel level, Entity entity) {
        // Проверяем, является ли моб жителем или разбойником
        if (entity.getType() == EntityType.VILLAGER ||
                entity.getType() == EntityType.PILLAGER ||
                entity.getType() == EntityType.VINDICATOR ||
                entity.getType() == EntityType.EVOKER ||
                entity.getType() == EntityType.ILLUSIONER ||
                entity.getType() == EntityType.WANDERING_TRADER) {

            // Превращаем в лягушку
            Frog frog = EntityType.FROG.create(level);
            if (frog != null) {
                frog.copyPosition(entity);
                level.addFreshEntity(frog);
                entity.discard();
            }
        }
        // Для всех остальных мобов (кроме игроков)
        else if (!(entity instanceof Player)) {
            // Превращаем в курицу
            Chicken chicken = EntityType.CHICKEN.create(level);
            if (chicken != null) {
                chicken.copyPosition(entity);
                level.addFreshEntity(chicken);
                entity.discard();
            }
        }

        // Визуальные эффекты трансформации
        spawnEffects(level, entity);
    }

    private static void spawnEffects(ServerLevel level, Entity entity) {
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        // Частицы
        level.sendParticles(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(),
                x, y, z, 36, 0.5, 0.5, 0.5, 0.05f);

        // Звуки
        if (entity instanceof Bee) {
            level.playSound(null, x, y, z,
                    SoundEvents.LLAMA_SPIT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        } else {
            level.playSound(null, x, y, z,
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    // Остальные методы остаются без изменений
    private static void processItemDrops(ServerLevel level, Entity entity, JsonObject dropsConfig) {
        RandomSource random = level.random;
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        for (var entry : dropsConfig.entrySet()) {
            JsonObject itemConfig = entry.getValue().getAsJsonObject();

            String itemId = itemConfig.get("item").getAsString();
            int minCount = itemConfig.has("min_count") ? itemConfig.get("min_count").getAsInt() : 1;
            int maxCount = itemConfig.has("max_count") ? itemConfig.get("max_count").getAsInt() : minCount;
            float chance = itemConfig.has("chance") ? itemConfig.get("chance").getAsFloat() : 1.0f;

            if (random.nextFloat() <= chance) {
                int count = minCount + random.nextInt(maxCount - minCount + 1);
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                if (item != null) {
                    ItemStack stack = new ItemStack(item, count);
                    ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
                    level.addFreshEntity(itemEntity);
                }
            }
        }
    }

    private static void processExperienceDrops(ServerLevel level, Entity entity, JsonObject xpConfig) {
        RandomSource random = level.random;
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        int minXp = xpConfig.has("min") ? xpConfig.get("min").getAsInt() : 0;
        int maxXp = xpConfig.has("max") ? xpConfig.get("max").getAsInt() : minXp;
        float chance = xpConfig.has("chance") ? xpConfig.get("chance").getAsFloat() : 1.0f;

        if (random.nextFloat() <= chance && maxXp > 0) {
            int xpAmount = minXp + random.nextInt(maxXp - minXp + 1);
            level.addFreshEntity(new ExperienceOrb(level, x, y, z, xpAmount));
        }
    }

    private static void loadMobDropsConfig() {
        try {
            ResourceLocation folderPath = new ResourceLocation("wd", "hit_mob");
            var resourceManager = ServerLifecycleHooks.getCurrentServer().getResourceManager();

            Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                    folderPath.getPath(),
                    path -> path.getPath().endsWith(".json")
            );

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (InputStream inputStream = entry.getValue().open()) {
                    JsonObject config = JsonParser.parseReader(
                            new InputStreamReader(inputStream)
                    ).getAsJsonObject();

                    for (Map.Entry<String, ?> mobEntry : config.entrySet()) {
                        MOB_DROPS.put(mobEntry.getKey(), (JsonObject) mobEntry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}