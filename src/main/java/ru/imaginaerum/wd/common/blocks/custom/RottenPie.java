package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import ru.imaginaerum.wd.common.effects.EffectsWD;
import ru.imaginaerum.wd.common.particles.ModParticles;
import ru.imaginaerum.wd.common.sounds.CustomSoundEvents;

public class RottenPie extends Block {

    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

    public RottenPie(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D);
    private static final VoxelShape SHAPE_1 = Shapes.or(
            box(1.0D, 0.0D, 6.0D, 15.0D, 8.0D, 15.0D),
            box(8.0D, 0.0D, 1.0D, 15.0D, 8.0D, 6.0D));
    private static final VoxelShape SHAPE_2 = Block.box(1.0D, 0.0D, 6.0D, 15.0D, 8.0D, 15.0D);
    private static final VoxelShape SHAPE_3 = Block.box(1.0D, 0.0D, 6.0D, 7.0D, 8.0D, 15.0D);

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        ItemStack itemstack = player.getItemInHand(hand);
        ItemStack stackHand = player.getItemInHand(hand);
        Item item = stackHand.getItem();
        if (level.isClientSide) {
            if (eat(level, pos, state, player).consumesAction()) {

                return InteractionResult.SUCCESS;
            }

            if (itemstack.isEmpty()) {
                return InteractionResult.CONSUME;
            }
        }

        return eat(level, pos, state, player);
    }

    protected static InteractionResult eat(LevelAccessor world, BlockPos blockPos, BlockState blockState, Player player) {
        if (!player.canEat(false)) {
            return InteractionResult.PASS;
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
            player.awardStat(Stats.EAT_CAKE_SLICE);
            player.getFoodData().eat(6, 0.8F);
            world.playSound((Player) null, blockPos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 1.0F, 1.0F);
            int i = blockState.getValue(STAGE);
            world.gameEvent(player, GameEvent.EAT, blockPos);
            if (i < 3) {
                world.setBlock(blockPos, blockState.setValue(STAGE, Integer.valueOf(i + 1)), 3);
            } else {
                world.removeBlock(blockPos, false);
                world.gameEvent(player, GameEvent.BLOCK_DESTROY, blockPos);
            }
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        int stage = state.getValue(STAGE);
        return switch (stage) {
            case 1 -> SHAPE_1;
            case 2 -> SHAPE_2;
            case 3 -> SHAPE_3;
            default -> SHAPE;
        };
    }


    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource source) {
        super.animateTick(state, level, pos, source);
        VoxelShape voxelshape = this.getShape(state, level, pos, CollisionContext.empty());
        level.playLocalSound((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, CustomSoundEvents.FLIES.get(), SoundSource.BLOCKS, 0.5F, source.nextFloat() * 0.4F + 0.8F, false);
        Vec3 vec3 = voxelshape.bounds().getCenter();
        double d0 = (double) pos.getX() + vec3.x;
        double d1 = (double) pos.getZ() + vec3.z;

        for (int i = 0; i < 3; ++i) {
            if (source.nextBoolean()) {
                level.addParticle(ModParticles.FLIES.get(), d0 + source.nextDouble() / 10.0D, (double) pos.getY() + (1D - source.nextDouble()), d1 + source.nextDouble() / 10.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
