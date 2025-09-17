package ru.imaginaerum.wd.common.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;


public class DragolitGrid extends IronBarsBlock {
    public DragolitGrid(Properties properties) {
        super(properties);
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
}
