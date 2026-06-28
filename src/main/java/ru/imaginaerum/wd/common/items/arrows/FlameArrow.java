package ru.imaginaerum.wd.common.items.arrows;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.PlayMessages;
import ru.imaginaerum.wd.common.blocks.custom.CandleWizardPie;
import ru.imaginaerum.wd.common.blocks.custom.WizardPie;
import ru.imaginaerum.wd.common.items.ItemsWD;

public class FlameArrow extends AbstractArrow {

    public FlameArrow(EntityType<? extends FlameArrow> p_37411_, Level p_37412_) {
        super(p_37411_, p_37412_);
    }

    public FlameArrow(Level p_37419_, LivingEntity p_37420_) {
        super(EntityTypeInit.FLAME_ARROW.get(), p_37420_, p_37419_);
    }

    public FlameArrow(Level p_37414_, double p_37415_, double p_37416_, double p_37417_) {
        super(EntityTypeInit.FLAME_ARROW.get(), p_37415_, p_37416_, p_37417_, p_37414_);
    }

    public FlameArrow(PlayMessages.SpawnEntity spawnEntity, Level world) {
        this(EntityTypeInit.FLAME_ARROW.get(), world);
    }

    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.inGround) {
            this.level().addParticle(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }

    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ItemsWD.FLAME_ARROW.get());
    }




    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);

        if (this.level().isClientSide) return;

        BlockPos pos = hit.getBlockPos();
        var state = this.level().getBlockState(pos);

// 🕯 Свечи
        if (state.getBlock() instanceof CandleBlock) {
            if (!state.getValue(BlockStateProperties.LIT)) {
                this.level().setBlock(pos,
                        state.setValue(BlockStateProperties.LIT, true),
                        3
                );
            }
            return;
        }

// 🧁 Торт со свечой
        if (state.getBlock() instanceof CandleCakeBlock) {
            if (!state.getValue(BlockStateProperties.LIT)) {
                this.level().setBlock(pos,
                        state.setValue(BlockStateProperties.LIT, true),
                        3
                );
            }
            return;
        }
        if (state.getBlock() instanceof CandleWizardPie) {
            if (!state.getValue(BlockStateProperties.LIT)) {
                this.level().setBlock(pos,
                        state.setValue(BlockStateProperties.LIT, true),
                        3
                );
            }
            return;
        }

// 🔥 Костёр
        if (state.getBlock() instanceof CampfireBlock) {
            if (!state.getValue(CampfireBlock.LIT)) {
                this.level().setBlock(pos,
                        state.setValue(CampfireBlock.LIT, true),
                        3
                );
            }
            return;
        }
        // Проверяем TNT
        if (state.is(Blocks.TNT)) {
            this.discard();
            // Удаляем блок
            this.level().removeBlock(pos, false);

            // Взрыв в точке TNT
            this.level().explode(
                    this.getOwner(),
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    4.0F,
                    Level.ExplosionInteraction.BLOCK
            );

            return;
        }

        // обычное поведение с огнём
        Entity entity = this.getOwner();
        if (!(entity instanceof Mob) || net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level(), entity)) {
            BlockPos firePos = pos.relative(hit.getDirection());
            if (this.level().isEmptyBlock(firePos)) {
                this.level().setBlockAndUpdate(firePos, BaseFireBlock.getState(this.level(), firePos));
            }
        }
    }
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        Entity target = result.getEntity();

        // 💥 TNT вагонетка
        if (target instanceof MinecartTNT tntCart) {

            if (!this.level().isClientSide) {

                double x = tntCart.getX();
                double y = tntCart.getY();
                double z = tntCart.getZ();
                this.discard();
                // удаляем вагонетку
                tntCart.discard();

                // взрыв
                this.level().explode(
                        this.getOwner(),
                        x, y, z,
                        4.0F,
                        Level.ExplosionInteraction.TNT
                );
            }

            return;
        }

        // 🔥 обычные живые существа
        if (target instanceof LivingEntity livingTarget) {
            livingTarget.setSecondsOnFire(10);
        }
    }
}