package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;

public class FireRod extends Block implements IPlantable {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public FireRod(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isEmptyBlock(pos.above())) {
            int height;
            for(height = 1; level.getBlockState(pos.below(height)).is(this); ++height) {
            }

            if (height < 3) {
                int age = state.getValue(AGE);
                if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(level, pos, state, true)) {
                    if (age == 15) {
                        level.setBlockAndUpdate(pos.above(), this.defaultBlockState());
                        net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos.above(), this.defaultBlockState());
                        level.setBlock(pos, state.setValue(AGE, 0), 4);
                    } else {
                        level.setBlock(pos, state.setValue(AGE, age + 1), 4);
                    }
                }
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockState blockBelow = world.getBlockState(pos.below());

        // Проверка поддержки растений блоком снизу (может вызвать NPE!)
        if (blockBelow.canSustainPlant(world, pos.below(), Direction.UP, this)) {
            return true;
        }

        // Если блок ниже такой же
        if (blockBelow.is(this)) {
            return true;
        }

        // Проверка специальных блоков
        if (blockBelow.is(BlockTags.NYLIUM) || blockBelow.is(Blocks.NETHERRACK) ||
                blockBelow.is(Blocks.GRAVEL) || blockBelow.is(Blocks.BASALT) ||
                blockBelow.is(Blocks.BLACKSTONE) || blockBelow.is(Blocks.STONE) ||
                blockBelow.is(Blocks.COBBLESTONE) || blockBelow.is(Blocks.ANDESITE) ||
                blockBelow.is(Blocks.DIORITE) || blockBelow.is(Blocks.GRANITE) ||
                blockBelow.is(Blocks.DEEPSLATE) || blockBelow.is(Blocks.TUFF) ||
                blockBelow.is(Blocks.CALCITE) || blockBelow.is(Blocks.BLACKSTONE) ||
                blockBelow.is(Blocks.BASALT) || blockBelow.is(Blocks.SMOOTH_BASALT)) {

            BlockPos belowPos = pos.below();

            // Проверка наличия лавы по соседству
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                FluidState neighborFluid = world.getFluidState(belowPos.relative(direction));
                if (neighborFluid.is(Fluids.LAVA) || neighborFluid.is(Fluids.FLOWING_LAVA)) {
                    return true;
                }
            }
        }

        // Магма-блок всегда поддерживает
        return blockBelow.is(Blocks.MAGMA_BLOCK);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public BlockState getPlant(BlockGetter world, BlockPos pos) {
        return defaultBlockState();
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        super.entityInside(state, world, pos, entity);
        // Исправленный код нанесения урона
        DamageSources damageSources = new DamageSources(world.registryAccess());
        entity.hurt(damageSources.inFire(), 1);
        entity.hurt(damageSources.generic(), 4);
        entity.setSecondsOnFire(5);
    }
}