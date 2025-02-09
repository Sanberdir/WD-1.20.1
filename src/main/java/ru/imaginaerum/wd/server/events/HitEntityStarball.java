package ru.imaginaerum.wd.server.events;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import ru.imaginaerum.wd.common.items.ItemsWD;
import ru.imaginaerum.wd.common.particles.ModParticles;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Random;

public class HitEntityStarball {
    private static final String CONFIG_FOLDER = "hit_mob";  // Папка с JSON файлами

    // Метод для загрузки и объединения всех JSON файлов из папки
    private static JsonObject loadConfig() {
        JsonObject mergedConfig = new JsonObject();

        try {
            // Указываем путь к папке с JSON-файлами
            ResourceLocation folderPath = new ResourceLocation("wd", CONFIG_FOLDER);
            var resourceManager = ServerLifecycleHooks.getCurrentServer().getResourceManager();

            // Получаем все ресурсы из папки hit_block
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
            e.printStackTrace();
        }

        return mergedConfig;
    }

    // Основной метод для обработки сущностей
    public static void hitEntity(Entity entity, ServerLevel world) {
        if (!world.isClientSide) {
            try {

                // Проверка, является ли сущность пчелой, и проигрывание звука плевка ламы
                if (entity instanceof Bee) {
                    world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.LLAMA_SPIT, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
                // Проверка типа сущности - игрок
                // Проверка на игрока для насыщения или нанесения урона
                if (entity instanceof Player player) {
                    int foodLevel = player.getFoodData().getFoodLevel();
                    if (foodLevel < 20) {
                        player.getFoodData().eat(4, 0.2F);
                        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY(), entity.getZ(), 36, 0.5, 0.5, 0.5, 0.05f);
                    } else {
                        // Получение стандартного источника урона
                        DamageSource damageSource = world.damageSources().generic();
                        player.hurt(damageSource, 3.0F);
                        world.sendParticles(ParticleTypes.ANGRY_VILLAGER, entity.getX(), entity.getY(), entity.getZ(), 36, 0.5, 0.5, 0.5, 0.05f);
                    }
                }
                // Проверка, является ли сущность крипером
                if ((entity.getType() == EntityType.VILLAGER)
                        || (entity.getType() == EntityType.PILLAGER)
                        || (entity.getType() == EntityType.VINDICATOR)
                        || (entity.getType() == EntityType.EVOKER)
                        || (entity.getType() == EntityType.WANDERING_TRADER)
                ) {
                    // Удаляем крипера
                    entity.discard();

                    world.sendParticles(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(), entity.getX(), entity.getY(), entity.getZ(), 36, 0.5, 0.5, 0.5, 0.05f);
                    // Создаём лягушку на месте крипера
                    Entity frog = EntityType.FROG.create(world);
                    if (frog != null) {
                        frog.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                        world.addFreshEntity(frog);

                        // Проигрываем звук создания лягушки
                        world.playSound(null, frog.blockPosition(), SoundEvents.FROG_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    }
                    return; // Завершаем выполнение метода
                }

                // Загружаем объединенный конфиг из всех JSON файлов
                JsonObject jsonConfig = loadConfig();

                JsonObject entities = jsonConfig.getAsJsonObject("entities");

                // Получение информации об entity
                String entityId = EntityType.getKey(entity.getType()).toString();
                if (!entities.has(entityId)
                        && !(entity instanceof Player player)
                        && !(entity instanceof WitherBoss witherBoss)
                        && !(entity instanceof EnderDragon enderDragon)) {

                    // Создаем координаты для спавна
                    double x = entity.getX();
                    double y = entity.getY();
                    double z = entity.getZ();

                    // Удаляем текущую сущность
                    entity.discard();

                    // Проверяем, является ли сущность НЕ летающей
                    if (!(entity instanceof FlyingMob)&&!(entity instanceof Boat)&&!(entity instanceof Dolphin)) {
                        // Превращаем наземных существ в кролика
                        Rabbit rabbit = EntityType.RABBIT.create(world);
                        if (rabbit != null) {
                            rabbit.setPos(x, y, z);
                            world.addFreshEntity(rabbit);

                            // Эффект частиц
                            world.sendParticles(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(), x, y, z, 36, 0.5, 0.5, 0.5, 0.05f);
                        }
                    }
                    else if (entity instanceof Boat) {
                        // Превращаем наземных существ в кролика
                        Dolphin dolphin = EntityType.DOLPHIN.create(world);
                        if (dolphin != null) {
                            dolphin.setPos(x, y, z);
                            world.addFreshEntity(dolphin);

                            // Эффект частиц
                            world.sendParticles(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(), x, y, z, 36, 0.5, 0.5, 0.5, 0.05f);
                        }
                    }
                    else if (entity instanceof Dolphin) {
                        // Превращаем наземных существ в кролика
                        Cod cod = EntityType.COD.create(world);
                        if (cod != null) {
                            cod.setPos(x, y, z);
                            world.addFreshEntity(cod);

                            // Эффект частиц
                            world.sendParticles(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(), x, y, z, 36, 0.5, 0.5, 0.5, 0.05f);
                        }
                    }
                    else {
                        // Превращаем летающих в курицу
                        Chicken chicken = EntityType.CHICKEN.create(world);
                        if (chicken != null) {
                            chicken.setPos(x, y, z);
                            world.addFreshEntity(chicken);

                            // Эффект частиц
                            world.sendParticles(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(), x, y, z, 36, 0.5, 0.5, 0.5, 0.05f);
                        }
                    }


                    return;
                }

                JsonObject entityData = entities.getAsJsonObject(entityId);
                JsonElement dropsElement = entityData.get("drops");
                if (new Random().nextFloat() <= 0.30f) {
                    ItemStack sparklingPollenStack = new ItemStack(ItemsWD.SPARKLING_POLLEN.get(), 1); // 1 штука
                    ItemEntity sparklingPollenEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), sparklingPollenStack);
                    world.addFreshEntity(sparklingPollenEntity);
                }
                // Выпадение сфер опыта
                JsonObject experience = entityData.getAsJsonObject("experience");
                if (experience != null) {
                    int minExp = experience.get("min").getAsInt();
                    int maxExp = experience.get("max").getAsInt();
                    int expCount = new Random().nextInt((maxExp - minExp) + 1) + minExp;

                    // Создание сфер опыта
                    for (int i = 0; i < expCount; i++) {
                        ExperienceOrb experienceOrb = new ExperienceOrb(world, entity.getX(), entity.getY(), entity.getZ(), expCount);
                        world.addFreshEntity(experienceOrb);
                    }
                }
                if (dropsElement != null && dropsElement.isJsonArray()) {
                    dropsElement.getAsJsonArray().forEach(dropElement -> {
                        JsonObject dropData = dropElement.getAsJsonObject();
                        String item = dropData.get("item").getAsString();
                        int minCount = dropData.get("min_count").getAsInt();  // Минимальное количество
                        int maxCount = dropData.get("max_count").getAsInt();  // Максимальное количество
                        float chance = dropData.get("chance").getAsFloat();

                        // Проверка шанса выпадения
                        if (new Random().nextFloat() <= chance) {

                            // Генерация случайного количества предметов в диапазоне от minCount до maxCount
                            int count = new Random().nextInt((maxCount - minCount) + 1) + minCount;
                            ItemStack dropStack = new ItemStack(
                                    ForgeRegistries.ITEMS.getValue(new ResourceLocation(item)), count);
                            ItemEntity dropEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), dropStack);
                            world.addFreshEntity(dropEntity);
                        }

                    });
                }

                // Деспаун сущности
                entity.discard();
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                // Частицы
                Random random = new Random();
                for (int i = 0; i < 10; i++) {
                    double offsetX = random.nextDouble() - 0.5;
                    double offsetY = random.nextDouble() * 0.5;
                    double offsetZ = random.nextDouble() - 0.5;
                    world.sendParticles(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(), entity.getX(), entity.getY(), entity.getZ(), 36, 0.5, 0.5, 0.5, 0.05f);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}