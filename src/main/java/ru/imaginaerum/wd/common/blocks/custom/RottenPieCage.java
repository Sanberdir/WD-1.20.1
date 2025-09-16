package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.imaginaerum.wd.common.particles.ModParticles;
import ru.imaginaerum.wd.common.sounds.CustomSoundEvents;

public class RottenPieCage extends Block {
    public RottenPieCage(Properties properties) {
        super(properties);
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
