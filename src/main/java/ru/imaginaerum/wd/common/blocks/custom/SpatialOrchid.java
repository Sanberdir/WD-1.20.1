package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;


public class SpatialOrchid extends Block {
    public SpatialOrchid(Properties properties) {
        super(properties);
    }
    private static final int TELEPORT_RADIUS = 300;

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Запланировать тик через 10 тиков, если он еще не запланирован
            if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
                level.scheduleTick(pos, this, 6);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Увеличиваем область проверки до 1 блока во все стороны, чтобы захватить игрока, который мог немного сместиться
        AABB searchArea = new AABB(pos).inflate(1.0);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, searchArea);

        if (!entities.isEmpty()) {
            // Телепортируем всех сущностей в области, а не только первую
            for (Entity entity : entities) {
                teleportEntityRandomly(level, entity);
            }
        }

        // Также проверить, может ли блок выжить (существующая логика)
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    private void teleportEntityRandomly(ServerLevel level, Entity entity) {
        RandomSource random = level.getRandom();
        // Попробуем найти подходящее место для телепортации
        int x = entity.blockPosition().getX() + random.nextInt(TELEPORT_RADIUS * 2) - TELEPORT_RADIUS;
        int z = entity.blockPosition().getZ() + random.nextInt(TELEPORT_RADIUS * 2) - TELEPORT_RADIUS;
        int y = level.getHeight() - 1; // Начинаем сверху (наибольшая высота в этом чанке)
        BlockPos candidatePos = new BlockPos(x, y, z);

        // Проверяем каждую позицию сверху вниз
        while (candidatePos.getY() > level.getMinBuildHeight()) {
            if (isValidTeleportLocation(level, candidatePos)) {
                entity.teleportTo(candidatePos.getX() + 0.5, candidatePos.getY() + 1, candidatePos.getZ() + 0.5);
                return; // Перемещаем игрока и выходим из цикла
            }
            candidatePos = candidatePos.below(); // Переходим ниже, если текущая позиция не подходит
        }
    }

    private boolean isValidTeleportLocation(Level level, BlockPos pos) {
        return level.getBlockState(pos).isSolid() && // Подходящая твёрдая поверхность
                level.getBlockState(pos.above()).isAir() && // Над поверхностью воздух
                level.getBlockState(pos.above(2)).isAir(); // Достаточно пространства для сущности
    }

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource source) {
        VoxelShape voxelshape = this.getShape(state, level, pos, CollisionContext.empty());
        Vec3 vec3 = voxelshape.bounds().getCenter();
        double d0 = (double)pos.getX() + vec3.x;
        double d1 = (double)pos.getZ() + vec3.z;
        for(int i = 0; i < 3; ++i) {
            if (source.nextBoolean()) {
                level.addParticle(ParticleTypes.PORTAL, d0 + source.nextDouble() / 5.0D, (double)pos.getY() + (0.5D - source.nextDouble()), d1 + source.nextDouble() / 5.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    public BlockState updateShape(BlockState currentBlockState, Direction direction, BlockState neighborBlockState, LevelAccessor world, BlockPos currentPos, BlockPos neighborPos) {
        if (!currentBlockState.canSurvive(world, currentPos)) {
            world.scheduleTick(currentPos, this, 1);
        }
        return super.updateShape(currentBlockState, direction, neighborBlockState, world, currentPos, neighborPos);
    }

    public boolean canSurvive(BlockState state, LevelReader levelReader, BlockPos blockPos) {
        BlockState belowBlockState = levelReader.getBlockState(blockPos.below());
        return belowBlockState.is(Blocks.END_STONE) || belowBlockState.is(Blocks.END_STONE_BRICKS);
    }
}
