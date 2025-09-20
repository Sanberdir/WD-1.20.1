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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.wd.common.items.ItemsWD;

import java.util.Random;

@Mixin(AbstractPiglin.class)
public class AbstractPiglinMixin extends Monster {

    @Unique
    private static final EntityDataAccessor<Boolean> PREVENT_ZOMBIFICATION = SynchedEntityData.defineId(AbstractPiglin.class, EntityDataSerializers.BOOLEAN);

    protected AbstractPiglinMixin(EntityType<? extends Monster> p_33002_, Level p_33003_) {
        super(p_33002_, p_33003_);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void onDefineSynchedData(CallbackInfo info) {
        ((AbstractPiglin) (Object) this).getEntityData().define(PREVENT_ZOMBIFICATION, false);
    }

    public boolean isPreventZombification() {
        return ((AbstractPiglin) (Object) this).getEntityData().get(PREVENT_ZOMBIFICATION);
    }

    public void setPreventZombification(boolean preventZombification) {
        ((AbstractPiglin) (Object) this).getEntityData().set(PREVENT_ZOMBIFICATION, preventZombification);
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
        if (player.getMainHandItem().getItem() == ItemsWD.NETHER_GROG.get() && !this.isPreventZombification()) {
            Random random = new Random();

            // 70% шанс на успешное взаимодействие
            if (random.nextFloat() < 0.7) {
                // Если успех, предотвращаем зомбификацию
                this.setPreventZombification(true);
                if (level() instanceof ServerLevel serverLevel) {
                    // Отправляем частицы на позицию пиглина, а не игрока
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            this.getX(), this.getY() + 1, this.getZ(),  // Позиция пиглина
                            10, 0.5, 0.5, 0.5, 0.1);
                }
                this.playSound(SoundEvents.PIGLIN_ADMIRING_ITEM, 1.0F, 1.0F);

                // Уменьшаем количество предметов
                if (!player.isCreative()) {
                    player.getMainHandItem().shrink(1);
                }

                return InteractionResult.SUCCESS;
            } else {
                // Если неудачно, проигрываем звук зомби
                this.playSound(SoundEvents.ZOMBIFIED_PIGLIN_ANGRY, 1.0F, 1.0F);

                // Преобразуем пиглина в зомби-пиглина
                if (!this.level().isClientSide) {
                    ZombifiedPiglin zombifiedPiglin = EntityType.ZOMBIFIED_PIGLIN.create(this.level());
                    zombifiedPiglin.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());

                    // Применяем атрибуты, такие как малыш
                    if (this.isBaby()) {
                        zombifiedPiglin.setBaby(true);
                    }

                    // Копируем данные из оригинального пиглина в зомби-пиглина
                    zombifiedPiglin.setInvulnerable(this.isInvulnerable());
                    zombifiedPiglin.setItemSlot(EquipmentSlot.HEAD, this.getItemBySlot(EquipmentSlot.HEAD));
                    zombifiedPiglin.setItemSlot(EquipmentSlot.CHEST, this.getItemBySlot(EquipmentSlot.CHEST));
                    zombifiedPiglin.setItemSlot(EquipmentSlot.LEGS, this.getItemBySlot(EquipmentSlot.LEGS));
                    zombifiedPiglin.setItemSlot(EquipmentSlot.FEET, this.getItemBySlot(EquipmentSlot.FEET));

                    // Копируем оружие
                    zombifiedPiglin.setItemInHand(InteractionHand.MAIN_HAND, this.getItemInHand(InteractionHand.MAIN_HAND));
                    zombifiedPiglin.setItemInHand(InteractionHand.OFF_HAND, this.getItemInHand(InteractionHand.OFF_HAND));
                    // Осуществляем трансформацию
                    this.discard();  // Удаляем старую сущность

                    this.level().addFreshEntity(zombifiedPiglin); // Добавляем новый зомби-пиглин в мир

                    // Частицы для зомби-пиглина
                    if (level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                                zombifiedPiglin.getX(), zombifiedPiglin.getY() + 1, zombifiedPiglin.getZ(),
                                10, 0.5, 0.5, 0.5, 0.1);
                    }
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
}
