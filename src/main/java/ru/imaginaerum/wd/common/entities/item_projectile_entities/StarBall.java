package ru.imaginaerum.wd.common.entities.item_projectile_entities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.blocks.custom.AppleLeavesStages;
import ru.imaginaerum.wd.common.entities.ModEntities;
import ru.imaginaerum.wd.common.items.ItemsWD;
import ru.imaginaerum.wd.server.events.HitAppleStarBall;
import ru.imaginaerum.wd.server.events.HitBlockStarBall;
import ru.imaginaerum.wd.server.events.HitEntityStarball;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class StarBall extends AbstractHurtingProjectileMod implements ItemSupplier {

    public StarBall(PlayMessages.SpawnEntity packet, Level world) {
        super(ModEntities.STAR_BALL.get(), world);
    }

    public StarBall(EntityType<? extends StarBall> type, Level world) {
        super(type, world);
    }
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);

        Entity entity = hitResult.getEntity();
        if (!this.level().isClientSide) {
            this.discard(); // Удаляем текущий объект

            // Проверяем, является ли уровень серверным
            if (this.level() instanceof ServerLevel serverLevel) {
                // Вызываем статический метод из HitEntityStarball
                HitEntityStarball.hitEntity(entity, serverLevel);
            }
        }
        // Дополнительная логика, если требуется
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (!this.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockState state = serverLevel.getBlockState(blockPos);

            // Стандартное поведение
            HitAppleStarBall.handle(serverLevel, blockPos);
                // Стандартное поведение
                HitBlockStarBall.hitBlock(serverLevel, blockPos,
                        blockPos.getX(), blockPos.getY(), blockPos.getZ());


            WD.queueServerWork(1, () -> this.discard());
        }
    }


    @Override
    protected void onInsideBlock(BlockState state) {
        super.onInsideBlock(state);
        if (!this.level().isClientSide) {
            // Получение уровня и позиции блока
            ServerLevel serverLevel = (ServerLevel) this.level();
            BlockPos blockPos = this.blockPosition(); // Получаем позицию объекта
            // Вызов метода из HitBlockStarBall
            HitBlockStarBall.hitBlock(serverLevel, blockPos, blockPos.getX(),blockPos.getY(),blockPos.getZ());
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ItemStack getItem() {
        return new ItemStack(ItemsWD.STAR_BALL.get());
    }
}