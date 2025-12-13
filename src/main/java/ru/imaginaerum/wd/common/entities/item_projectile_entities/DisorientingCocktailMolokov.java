package ru.imaginaerum.wd.common.entities.item_projectile_entities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;
import ru.imaginaerum.wd.common.entities.ModEntities;
import ru.imaginaerum.wd.common.items.ItemsWD;

public class DisorientingCocktailMolokov extends ThrowableItemProjectile {

    // 1. ИСПРАВЛЕННЫЙ конструктор: тип должен соответствовать MilkBottle
    public DisorientingCocktailMolokov(EntityType<? extends DisorientingCocktailMolokov> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public DisorientingCocktailMolokov(Level pLevel, LivingEntity pShooter) {
        // 2. Используйте вашу зарегистрированную EntityType
        super(ModEntities.DISORIENTING_COCKTAIL_MOLOKOV.get(), pShooter, pLevel);
    }

    public DisorientingCocktailMolokov(Level pLevel, double pX, double pY, double pZ) {
        super(ModEntities.DISORIENTING_COCKTAIL_MOLOKOV.get(), pX, pY, pZ, pLevel);
    }

    private ParticleOptions getParticle() {
        ItemStack itemstack = this.getItemRaw();
        return (ParticleOptions)(itemstack.isEmpty() ? ParticleTypes.ITEM_SNOWBALL : new ItemParticleOption(ParticleTypes.ITEM, itemstack));
    }

    @Override
    protected Item getDefaultItem() {
        return ItemsWD.DISORIENTING_COCKTAIL_MOLOKOV.get();
    }

    @Override
    public void handleEntityEvent(byte id) {
        // Не вызываем супер, чтобы стандартные частицы не появились
        if (id == 3) {
            // Можно вызвать свои цветные частицы, если нужно
        }
    }

    protected void onHitEntity(EntityHitResult pResult) {
        Entity entity = pResult.getEntity();
        int i = entity instanceof Blaze ? 3 : 0;
        entity.hurt(this.damageSources().thrown(this, this.getOwner()), (float)i);
    }

    protected void onHit(HitResult pResult) {
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }
    }
}