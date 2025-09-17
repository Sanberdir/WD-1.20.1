package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.wd.common.blocks.entity.DragoliteCageBlockEntity;
import ru.imaginaerum.wd.common.blocks.entity.RoseMurdererBlockEntity;
import ru.imaginaerum.wd.common.items.ItemsWD;
import ru.imaginaerum.wd.common.items.custom.SoulStone;
import ru.imaginaerum.wd.common.particles.ModParticles;

public class DragoliteCage extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty SPARKING = IntegerProperty.create("sparking", 0, 5);
    public static final IntegerProperty SOULS = IntegerProperty.create("souls", 0, 2);
    public DragoliteCage(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(SPARKING, 0)
                .setValue(SOULS, 0));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SPARKING, SOULS);
    }
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }
    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(world, pos, state, entity);

        if (entity instanceof LivingEntity livingEntity && livingEntity.getMobType() == MobType.UNDEAD) {
            // Наносим урон 4 сердца
            entity.hurt(new DamageSource(world.registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 8);
        }
    }

    // Добавляем обработку смерти моба
    public static void onEntityDeath(LivingEntity entity, DamageSource source) {
        if (source.getMsgId().equals("generic") && entity.getMobType() == MobType.UNDEAD) {
            Level world = entity.level();
            if (!world.isClientSide && world instanceof ServerLevel serverWorld) {
                int xp = 7 + world.random.nextInt(4); // 7–10 XP
                net.minecraft.world.entity.ExperienceOrb.award(
                        serverWorld,
                        entity.position(),
                        xp
                );
            }
        }
    }
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity != null) {
            CompoundTag blockTag = blockEntity.getPersistentData();
            String soulFirst = blockTag.getString("soul_first");
            String soulSecond = blockTag.getString("soul_second");

            // Проверяем, что теги не пустые и равны между собой
            if (!soulFirst.isEmpty() && soulFirst.equals(soulSecond)) {
                // Уменьшаем уровень SPARKING с шансом 30%
                if (state.getValue(SPARKING) > 0) {
                    for (int i = 0; i < 1; i++) { // спауним 1 моба для примера
                        double x = pos.getX() + (random.nextDouble() * 10 - 5); // случайная позиция в радиусе 5 блоков
                        double y = pos.getY() + (random.nextInt(11) - 5);
                        double z = pos.getZ() + (random.nextDouble() * 10 - 5);
                        BlockPos spawnPos = new BlockPos((int) x, (int) y, (int) z);

                        // Получаем тип сущности по ID из soulFirst
                        EntityType<?> entityType = EntityType.byString(soulFirst).orElse(null);
                        if (entityType != null) {
                            Entity entity = entityType.create(level);
                            if (entity != null) {
                                boolean isWaterAnimal = entity instanceof WaterAnimal;

                                if (isWaterAnimal) {
                                    // Проверка на наличие воды
                                    if (level.getBlockState(spawnPos).getFluidState().isSource()) {
                                        spawnEntityInWater(level, spawnPos, entity, state, pos, random);
                                    }
                                } else {
                                    // Проверка на наличие подходящей поверхности
                                    if (level.getBlockState(spawnPos.below()).isSolid()) {
                                        spawnEntityOnLand(level, spawnPos, entity, state, pos, random);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void spawnEntityOnLand(ServerLevel level, BlockPos spawnPos, Entity entity, BlockState state, BlockPos blockPos, RandomSource random) {
        // Получаем самую верхнюю позицию для спауна
        int highestY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnPos.getX(), spawnPos.getZ());
        BlockPos topPos = new BlockPos(spawnPos.getX(), highestY, spawnPos.getZ());

        // Перемещаем сущность на верхнюю точку
        entity.moveTo(topPos.getX(), topPos.getY(), topPos.getZ(), 0, 0);

        // Проверяем, что на этом месте можно разместить моба
        if (level.getBlockState(topPos).isAir() && level.getBlockState(topPos.below()).isSolid()) {
            level.addFreshEntity(entity);

            if (random.nextInt(100) < 45) { // шанс 45%
                int currentSparking = state.getValue(SPARKING);
                if (currentSparking > 0) {
                    level.setBlock(blockPos, state.setValue(SPARKING, currentSparking - 1), 3);
                    level.playSound(null, blockPos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1, 1);
                }
            }
        }
    }

    private void spawnEntityInWater(ServerLevel level, BlockPos spawnPos, Entity entity, BlockState state, BlockPos blockPos, RandomSource random) {
        entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        level.addFreshEntity(entity);
        if (random.nextInt(100) < 45) { // шанс 45%
            int currentSparking = state.getValue(SPARKING);
            if (currentSparking > 0) {
                level.setBlock(blockPos, state.setValue(SPARKING, currentSparking - 1), 3);
                level.playSound(null, blockPos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1, 1);
            }
        }
    }





    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource source) {
        super.animateTick(state, level, pos, source);

        int souls = state.getValue(SOULS);

        // Получаем BlockEntity
        BlockEntity blockEntity = level.getBlockEntity(pos);

        // Проверяем, является ли блок объектом DragoliteCageBlock
        if (state.getBlock() instanceof DragoliteCage) {
            if (blockEntity != null) {
                // Получаем теги из BlockEntity
                CompoundTag blockTag = blockEntity.getUpdateTag(); // Или используйте метод, который возвращает теги
                String soulFirst = blockTag.getString("soul_first");
                String soulSecond = blockTag.getString("soul_second");

                // Теперь проверяем теги
                if (souls == 2 && !soulFirst.isEmpty() && !soulSecond.isEmpty() && soulFirst.equals(soulSecond)) {
                    if (source.nextInt(100) == 0) {
                        level.playSound((Player) null, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }

                    double d0 = (double) pos.getX() + 0.5D + (0.5D - source.nextDouble());
                    double d1 = (double) pos.getY() + 0.3D;
                    double d2 = (double) pos.getZ() + 0.5D + (0.5D - source.nextDouble());
                    double d3 = (double) source.nextFloat() * 0.04D;
                    level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, d0, d1, d2, 0.0D, d3, 0.0D);
                }
            }
        }


        // Проверка для SPARKING
        if (state.getValue(SPARKING) > 0) {
            if (source.nextInt(100) == 0) {
                level.playSound((Player) null, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            double d0 = (double) pos.getX() + 0.5D + (0.5D - source.nextDouble());
            double d1 = (double) pos.getY() + 1D;
            double d2 = (double) pos.getZ() + 0.5D + (0.5D - source.nextDouble());
            double d3 = (double) source.nextFloat() * 0.04D;
            level.addParticle(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(), d0, d1, d2, 0.0D, d3, 0.0D);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        if (player.getItemInHand(hand).is(ItemsWD.SPARKLING_POLLEN.get())) {
            handleSparklingPollen(state, level, pos, player, hand);
        }

        if (player.getItemInHand(hand).getItem() instanceof SoulStone) {
            handleSoulStone(state, level, pos, player, hand);
        }
        // Проверка на предмет RobinStick
        if (player.getItemInHand(hand).is(ItemsWD.ROBIN_STICK.get())) {
            handleRobinStick(state, level, pos, player, hand);
            player.swing(hand);
        }

        level.sendBlockUpdated(pos, state, state, 3);
        level.setBlocksDirty(pos, state, state);
        return super.use(state, level, pos, player, hand, blockHitResult);
    }
    private void handleRobinStick(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                CompoundTag blockTag = blockEntity.getPersistentData();
                String soulFirst = blockTag.getString("soul_first");
                String soulSecond = blockTag.getString("soul_second");

                int currentSouls = state.getValue(SOULS);
                if (currentSouls == 2) {
                    // Уменьшаем SOULS на 1
                    level.setBlock(pos, state.setValue(SOULS, 1), 3);

                    // Создаем SoulStone с нужными NBT данными
                    ItemStack soulStoneStack = new ItemStack(ItemsWD.SOUL_STONE.get());
                    CompoundTag nbt = soulStoneStack.getOrCreateTag();
                    nbt.putString("entity_type", soulSecond);

                    // Присваиваем имя сущности из soulSecond
                    String entityName = getEntityName(level, soulSecond);
                    nbt.putString("entity_name", entityName);

                    // Применяем дополнительные теги
                    chargeSoulStone(soulStoneStack);

                    soulStoneStack.setTag(nbt);
                    player.getInventory().add(soulStoneStack);

                    // Убираем Soul из тега
                    blockTag.remove("soul_second");

                    // Перезаписываем второй душой
                    if (blockEntity instanceof DragoliteCageBlockEntity dragoliteCageBlockEntity) {
                        dragoliteCageBlockEntity.setSoulSecond("");
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);

                } else if (currentSouls == 1) {
                    // Уменьшаем SOULS на 1
                    level.setBlock(pos, state.setValue(SOULS, 0), 3);

                    // Создаем SoulStone с нужными NBT данными
                    ItemStack soulStoneStack = new ItemStack(ItemsWD.SOUL_STONE.get());
                    CompoundTag nbt = soulStoneStack.getOrCreateTag();
                    nbt.putString("entity_type", soulFirst);

                    // Присваиваем имя сущности из soulFirst
                    String entityName = getEntityName(level, soulFirst);
                    nbt.putString("entity_name", entityName);

                    // Применяем дополнительные теги
                    chargeSoulStone(soulStoneStack);

                    soulStoneStack.setTag(nbt);
                    player.getInventory().add(soulStoneStack);

                    // Убираем Soul из тега
                    blockTag.remove("soul_first");

                    // Перезаписываем первый душой
                    if (blockEntity instanceof DragoliteCageBlockEntity dragoliteCageBlockEntity) {
                        dragoliteCageBlockEntity.setSoulFirst("");
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);

                }

                // Звук для получения SoulStone


            }
        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        // Проверяем, что игрок разрушает блок
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                CompoundTag blockTag = blockEntity.getPersistentData();
                String soulFirst = blockTag.getString("soul_first");
                String soulSecond = blockTag.getString("soul_second");

                int currentSouls = state.getValue(SOULS);

                // Если текущие души равны 2, выдаем оба камня душ
                if (currentSouls == 2) {
                    // Проверка на наличие soulFirst и soulSecond
                    if (!soulFirst.isEmpty()) {
                        // Создаем SoulStone для soulFirst
                        ItemStack soulStoneStackFirst = new ItemStack(ItemsWD.SOUL_STONE.get());
                        CompoundTag nbtFirst = soulStoneStackFirst.getOrCreateTag();
                        nbtFirst.putString("entity_type", soulFirst);
                        String entityNameFirst = getEntityName(level, soulFirst);
                        nbtFirst.putString("entity_name", entityNameFirst);

                        // Применяем дополнительные теги
                        chargeSoulStone(soulStoneStackFirst);

                        soulStoneStackFirst.setTag(nbtFirst);

                        // Выпадает SoulStone для soulFirst
                        ItemEntity itemEntityFirst = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, soulStoneStackFirst);
                        level.addFreshEntity(itemEntityFirst);

                        // Убираем Soul из тега
                        blockTag.remove("soul_first");

                        // Перезаписываем первый душой
                        if (blockEntity instanceof DragoliteCageBlockEntity dragoliteCageBlockEntity) {
                            dragoliteCageBlockEntity.setSoulFirst("");
                        }
                    }

                    if (!soulSecond.isEmpty()) {
                        // Создаем SoulStone для soulSecond
                        ItemStack soulStoneStackSecond = new ItemStack(ItemsWD.SOUL_STONE.get());
                        CompoundTag nbtSecond = soulStoneStackSecond.getOrCreateTag();
                        nbtSecond.putString("entity_type", soulSecond);
                        String entityNameSecond = getEntityName(level, soulSecond);
                        nbtSecond.putString("entity_name", entityNameSecond);

                        // Применяем дополнительные теги
                        chargeSoulStone(soulStoneStackSecond);

                        soulStoneStackSecond.setTag(nbtSecond);

                        // Выпадает SoulStone для soulSecond
                        ItemEntity itemEntitySecond = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, soulStoneStackSecond);
                        level.addFreshEntity(itemEntitySecond);

                        // Убираем Soul из тега
                        blockTag.remove("soul_second");

                        // Перезаписываем второй душой
                        if (blockEntity instanceof DragoliteCageBlockEntity dragoliteCageBlockEntity) {
                            dragoliteCageBlockEntity.setSoulSecond("");
                        }
                    }

                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);

                } else if (currentSouls == 1) {
                    // Если одна душа, выполняем как раньше
                    if (!soulFirst.isEmpty()) {
                        // Создаем SoulStone для soulFirst
                        ItemStack soulStoneStackFirst = new ItemStack(ItemsWD.SOUL_STONE.get());
                        CompoundTag nbtFirst = soulStoneStackFirst.getOrCreateTag();
                        nbtFirst.putString("entity_type", soulFirst);
                        String entityNameFirst = getEntityName(level, soulFirst);
                        nbtFirst.putString("entity_name", entityNameFirst);

                        // Применяем дополнительные теги
                        chargeSoulStone(soulStoneStackFirst);

                        soulStoneStackFirst.setTag(nbtFirst);

                        // Выпадает SoulStone для soulFirst
                        ItemEntity itemEntityFirst = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, soulStoneStackFirst);
                        level.addFreshEntity(itemEntityFirst);

                        // Убираем Soul из тега
                        blockTag.remove("soul_first");

                        // Перезаписываем первый душой
                        if (blockEntity instanceof DragoliteCageBlockEntity dragoliteCageBlockEntity) {
                            dragoliteCageBlockEntity.setSoulFirst("");
                        }
                    }

                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);

    }


    // Метод для присваивания тегов Charged и CustomModelData
    public static void chargeSoulStone(ItemStack stack) {
        stack.getOrCreateTag().putBoolean("Charged", true);
        stack.getOrCreateTag().putInt("CustomModelData", 1); // Обновляем модель
    }

    // Метод для извлечения имени сущности по типу
    private String getEntityName(Level level, String entityType) {
        // Ищем сущность по типу в мире
        EntityType<?> entityTypeObj = EntityType.byString(entityType).orElse(null);
        if (entityTypeObj != null) {
            // Создаем сущность из типа
            Entity entity = entityTypeObj.create(level);
            if (entity != null) {
                // Получаем имя сущности
                return entity.getName().getString();
            }
        }
        return "Unknown Entity"; // Если не удалось найти сущность или получить имя
    }


    private void handleSparklingPollen(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            int currentSparking = state.getValue(SPARKING);
            if (currentSparking < 5) {
                level.setBlock(pos, state.setValue(SPARKING, currentSparking + 1), 3);
                if (!player.isCreative()) {
                    player.getItemInHand(hand).shrink(1);
                }
                level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1, 1);
            }
        }
    }

    private void handleSoulStone(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        SoulStone soulStone = (SoulStone) player.getItemInHand(hand).getItem();
        if (soulStone.isCharged(player.getItemInHand(hand))) {
            if (!level.isClientSide) {
                int currentSoul = state.getValue(SOULS);
                if (currentSoul < 2) {
                    CompoundTag tag = player.getItemInHand(hand).getOrCreateTag();
                    String entityType = tag.getString("entity_type");

                    level.setBlock(pos, state.setValue(SOULS, currentSoul + 1), 3);

                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity != null) {
                        handleSoulStorage(blockEntity, currentSoul, entityType);
                    }

                    if (!player.isCreative()) {
                        player.getItemInHand(hand).shrink(1);
                    }

                    level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1, 1);
                }
            }
        }
    }

    private void handleSoulStorage(BlockEntity blockEntity, int currentSoul, String entityType) {
        CompoundTag blockTag = blockEntity.getPersistentData();
        if (currentSoul == 0) {
            blockTag.putString("soul_first", entityType);
            if (blockEntity instanceof DragoliteCageBlockEntity dragoliteCageBlockEntity) {
                dragoliteCageBlockEntity.setSoulFirst(entityType);
            }
        } else if (currentSoul == 1) {
            blockTag.putString("soul_second", entityType);
            if (blockEntity instanceof DragoliteCageBlockEntity dragoliteCageBlockEntity) {
                dragoliteCageBlockEntity.setSoulSecond(entityType);
            }
        }
    }




    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(SPARKING, 0)
                .setValue(SOULS, 0);

    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new DragoliteCageBlockEntity(pPos, pState);

    }
}
