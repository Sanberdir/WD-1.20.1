package ru.imaginaerum.wd.server.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.custom.GoldenRose;
import ru.imaginaerum.wd.common.blocks.custom.RottenPie;
import ru.imaginaerum.wd.common.items.ItemsWD;
import ru.imaginaerum.wd.common.particles.ModParticles;
import ru.imaginaerum.wd.common.sounds.CustomSoundEvents;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class HitBlockStarBall {

    public static void hitBlock(ServerLevel level, BlockPos pos, double x, double y, double z) {
        if (level == null || pos == null) return;

        // Получение состояния блока
        BlockState state = level.getBlockState(pos);
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null) return;

        // Добавляем проверку на A_BLOCK_OF_SPARKING_POLLEN
        if (state.getBlock() == BlocksWD.A_BLOCK_OF_SPARKING_POLLEN.get()) {
            // Взрыв с силой 10 и типом break
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.explode(null, x, y, z, 10, Level.ExplosionInteraction.TNT);

            Random random = new Random();
            for (int i = 0; i < 10; i++) {
                double offsetX = random.nextDouble() - 0.5;
                double offsetY = random.nextDouble() * 0.5;
                double offsetZ = random.nextDouble() - 0.5;
                level.sendParticles(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(), pos.getX(), pos.getY(), pos.getZ(), 350, 5, 5, 5, 0.7f);
            }
            return; // Завершаем выполнение, так как взрыв произошел
        }

        if (state.getBlock() == BlocksWD.WIZARD_PIE.get()) {
            // Взрыв с силой 10 и типом break
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.explode(null, x, y, z, 4, Level.ExplosionInteraction.TNT);

            Random random = new Random();
            for (int i = 0; i < 10; i++) {
                double offsetX = random.nextDouble() - 0.5;
                double offsetY = random.nextDouble() * 0.5;
                double offsetZ = random.nextDouble() - 0.5;
                level.sendParticles(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(), pos.getX(), pos.getY(), pos.getZ(), 100, 2, 2, 2, 0.3f);
            }
            return; // Завершаем выполнение, так как взрыв произошел
        }
        if (state.getBlock() == BlocksWD.MEADOW_GOLDEN_FLOWER.get() && state.getValue(GoldenRose.ACTIVE)) {
            // Находим все сущности зомби-жителей в радиусе 5 блоков
            double radius = 5.0;
            List<Entity> nearbyEntities = level.getEntities(null, new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius));

            for (Entity entity : nearbyEntities) {
                if (entity instanceof ZombieVillager zombieVillager) {
                    // Превращаем зомби-жителя в жителя
                    Villager villager = EntityType.VILLAGER.create(level);
                    if (villager != null) {
                        villager.copyPosition(zombieVillager);  // Копируем позицию и ориентацию зомби-жителя
                        level.addFreshEntity(villager);  // Добавляем жителя в мир
                        zombieVillager.discard();  // Удаляем зомби-жителя
                    }
                }
            }

            // Эффект частиц для визуализации
            level.sendParticles(ModParticles.GOLDEN_FLOWER_PARTICLES.get(), pos.getX(), pos.getY(), pos.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.3f);
            level.playSound(null, pos, CustomSoundEvents.GOLDEN_FLOWER.get(), SoundSource.BLOCKS, 1.0f, 1.0f);

            BlockState newState = state.setValue(GoldenRose.ACTIVE, false);
            level.setBlock(pos, newState, 3);
            return; // Завершаем выполнение после превращения зомби-жителей
        }
        if (state.getBlock() == Blocks.VINE) {
            // Замените блок vine на воздух
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

            // Создайте крипера вместо блока vine
            Creeper creeper = EntityType.CREEPER.create(level);
            creeper.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
            level.addFreshEntity(creeper);

            // Проиграйте звук создания крипера
            level.playSound(null, creeper.blockPosition(), SoundEvents.CREEPER_HURT, SoundSource.BLOCKS, 1.0f, 1.0f);
            return; // Завершаем выполнение, так как взрыв произошел
        }
        if (state.getBlock() == BlocksWD.ROTTEN_PIE.get()) {
            int stage = state.getValue(RottenPie.STAGE);
            // Заменить блок на воздух
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

            // Определим количество зомби в зависимости от стадии
            int zombieCount = 0;
            switch (stage) {
                case 0:
                    zombieCount = 8; // На стадии 0 создаем 8 зомби
                    break;
                case 1:
                    zombieCount = 6; // На стадии 1 создаем 6 зомби
                    break;
                case 2:
                    zombieCount = 4; // На стадии 2 создаем 4 зомби
                    break;
                case 3:
                    zombieCount = 2; // На стадии 3 создаем 2 зомби
                    break;
            }

            // Создание зомби
            for (int i = 0; i < zombieCount; i++) {
                if (level.random.nextDouble() < 0.1) { // 10% шанс на зомби-жителя
                    ZombieVillager zombieVillager = EntityType.ZOMBIE_VILLAGER.create(level);
                    if (zombieVillager != null) {
                        // Размещаем зомби-жителя с небольшими случайными смещениями
                        double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
                        double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
                        zombieVillager.moveTo(pos.getX() + 0.5 + offsetX, pos.getY(), pos.getZ() + 0.5 + offsetZ, level.random.nextFloat() * 360.0f, 0.0f);
                        level.addFreshEntity(zombieVillager);
                    }
                } else { // 90% шанс на обычного зомби
                    Zombie zombie = EntityType.ZOMBIE.create(level);
                    if (zombie != null) {
                        // Размещаем зомби с небольшими случайными смещениями
                        double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
                        double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
                        zombie.moveTo(pos.getX() + 0.5 + offsetX, pos.getY(), pos.getZ() + 0.5 + offsetZ, level.random.nextFloat() * 360.0f, 0.0f);
                        level.addFreshEntity(zombie);
                    }
                }
            }

            // Проиграйте звук создания зомби
            level.playSound(null, pos, SoundEvents.ZOMBIE_DEATH, SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        // Загрузка конфигурации из JSON
        JsonObject config = loadConfig();
        if (config == null || !config.has(blockId.toString())) return;

        JsonObject blockConfig = config.getAsJsonObject(blockId.toString());

        // Эффект разрушения
        playBlockBreakAnimation(level, pos, state);

        // Частицы
        spawnParticles(level, pos);

        // Дроп предметов
        dropItems(level, pos, blockConfig);
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }

    private static void playBlockBreakAnimation(ServerLevel level, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getShape(level, pos);
        Vec3 center = shape.bounds().getCenter();
        level.levelEvent(2001, pos, Block.getId(state)); // Проигрывает анимацию ломания блока
    }

    private static void spawnParticles(ServerLevel level, BlockPos pos) {
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            double offsetX = random.nextDouble() - 0.5;
            double offsetY = random.nextDouble() * 0.5;
            double offsetZ = random.nextDouble() - 0.5;
            level.sendParticles(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(), pos.getX(), pos.getY(), pos.getZ(), 36, 0.5, 0.5, 0.5, 0.05f);
        }
    }

    private static void dropItems(ServerLevel level, BlockPos pos, JsonObject blockConfig) {
        if (!blockConfig.has("drops")) return;
        if (new Random().nextFloat() <= 0.30f) {
            ItemStack sparklingPollenStack = new ItemStack(ItemsWD.SPARKLING_POLLEN.get(), 1); // 1 штука
            Block.popResource(level, pos, sparklingPollenStack);
        }
        if (blockConfig.has("experience")) {
            int experienceAmount = blockConfig.get("experience").getAsInt();
            level.addFreshEntity(new ExperienceOrb(level, pos.getX(), pos.getY(), pos.getZ(), experienceAmount));
        }
        // Получаем массив дропов
        for (var drop : blockConfig.getAsJsonArray("drops")) {
            JsonObject itemConfig = drop.getAsJsonObject();
            String itemId = itemConfig.get("item").getAsString();

            int minCount = itemConfig.has("min_count") ? itemConfig.get("min_count").getAsInt() : 1;
            int maxCount = itemConfig.has("max_count") ? itemConfig.get("max_count").getAsInt() : minCount;

            // Генерируем случайное количество в пределах minCount и maxCount
            Random random = new Random();
            int count = random.nextInt(maxCount - minCount + 1) + minCount;

            float chance = itemConfig.has("chance") ? itemConfig.get("chance").getAsFloat() : 1.0f;

            if (random.nextFloat() <= chance) {
                ItemStack itemStack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId)), count);
                Block.popResource(level, pos, itemStack);
            }
            // Добавляем выпадение ItemsWD.SPARKLING_POLLEN с шансом 30%

        }
    }

    private static JsonObject loadConfig() {
        JsonObject mergedConfig = new JsonObject();

        try {
            // Указываем путь к папке с JSON-файлами
            ResourceLocation folderPath = new ResourceLocation("wd", "hit_block");
            var resourceManager = ServerLifecycleHooks.getCurrentServer().getResourceManager();

            // Получаем ресурсы в папке hit_block
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
}
