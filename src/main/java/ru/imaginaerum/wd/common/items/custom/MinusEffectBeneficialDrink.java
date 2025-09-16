package ru.imaginaerum.wd.common.items.custom;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class MinusEffectBeneficialDrink extends Item {
    public MinusEffectBeneficialDrink(Properties properties) {
        super(properties);
    }
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entityLiving) {
        if (entityLiving instanceof Player) {
            Player player = (Player) entityLiving;

            // Собираем эффекты для удаления в отдельный список
            List<MobEffect> effectsToRemove = new ArrayList<>();
            for (MobEffectInstance effectInstance : player.getActiveEffects()) {
                if (!effectInstance.getEffect().isBeneficial()) {
                    effectsToRemove.add(effectInstance.getEffect());
                    if (effectsToRemove.size() >= 2) {
                        break; // Останавливаемся после сбора двух эффектов
                    }
                }
            }

            // Удаляем собранные эффекты
            for (MobEffect effect : effectsToRemove) {
                player.removeEffect(effect);
            }
        }

        return super.finishUsingItem(stack, world, entityLiving);
    }
}
