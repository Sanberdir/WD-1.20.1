package ru.imaginaerum.wd.common.mixin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.wd.common.items.ItemsWD;

import java.util.Random;

@Mixin(Hoglin.class)
public class HoglinMixin extends Animal implements Enemy, HoglinBase {

    @Unique
    private static final EntityDataAccessor<Boolean> PREVENT_ZOMBIFICATION = SynchedEntityData.defineId(Hoglin.class, EntityDataSerializers.BOOLEAN);

    protected HoglinMixin(EntityType<? extends Animal> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
    }


    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void onDefineSynchedData(CallbackInfo info) {
        ((Hoglin) (Object) this).getEntityData().define(PREVENT_ZOMBIFICATION, false);
    }

    public boolean isPreventZombification() {
        return ((Hoglin) (Object) this).getEntityData().get(PREVENT_ZOMBIFICATION);
    }

    public void setPreventZombification(boolean preventZombification) {
        ((Hoglin) (Object) this).getEntityData().set(PREVENT_ZOMBIFICATION, preventZombification);
    }

    @Inject(method = "isImmuneToZombification()Z", at = @At(value = "RETURN"), cancellable = true)
    protected void isImmuneToZombification(CallbackInfoReturnable<Boolean> cir) {
        if (this.isPreventZombification()) {
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(false);
        }
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getMainHandItem().getItem() == ItemsWD.TINCTURE_FOR_PIGLIN.get() && !this.isPreventZombification()) {
            Random random = new Random();

            // 70% шанс на успешное взаимодействие
            if (random.nextFloat() < 0.7) {
                // Если успех, предотвращаем зомбификацию
                this.setPreventZombification(true);
                if (level() instanceof ServerLevel serverLevel) {
                    // Отправляем частицы на позицию пиглина, а не игрока
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            this.getX(), this.getY() + 1, this.getZ(),  // Позиция пиглина
                            10, 1, 1, 1, 0.1);
                }                this.playSound(SoundEvents.HOGLIN_AMBIENT, 1.0F, 1.0F);

                // Уменьшаем количество предметов
                if (!player.isCreative()) {
                    player.getMainHandItem().shrink(1);
                }

                return InteractionResult.SUCCESS;
            } else {
                // Если неудачно, проигрываем звук зомби
                this.playSound(SoundEvents.ZOGLIN_ANGRY, 1.0F, 1.0F);

                // Преобразуем пиглина в зомби-пиглина
                if (!this.level().isClientSide) {
                    Zoglin zombifiedPiglin = EntityType.ZOGLIN.create(this.level());
                    zombifiedPiglin.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());

                    // Применяем атрибуты, такие как малыш
                    if (this.isBaby()) {
                        zombifiedPiglin.setBaby(true);
                    }
                    if (level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                                zombifiedPiglin.getX(), zombifiedPiglin.getY() + 1, zombifiedPiglin.getZ(),
                                10, 0.5, 0.5, 0.5, 0.1);
                    }
                    // Копируем данные из оригинального пиглина в зомби-пиглина
                    zombifiedPiglin.setInvulnerable(this.isInvulnerable());

                    // Осуществляем трансформацию
                    this.discard();  // Удаляем старую сущность

                    this.level().addFreshEntity(zombifiedPiglin); // Добавляем новый зомби-пиглин в мир

                    // Частицы для зомби-пиглина
                }

                return InteractionResult.SUCCESS;  // Вернуть успешный результат
            }
        }
        return InteractionResult.PASS;  // Если не сработал ни один из условий
    }

    @Override
    public boolean save(CompoundTag tag) {
        tag.putBoolean("PreventZombification", this.isPreventZombification());
        return super.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("PreventZombification")) {
            this.setPreventZombification(tag.getBoolean("PreventZombification"));
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null;
    }

    @Override
    public int getAttackAnimationRemainingTicks() {
        return 0;
    }
}
