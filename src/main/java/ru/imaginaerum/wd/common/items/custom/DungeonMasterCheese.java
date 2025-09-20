package ru.imaginaerum.wd.common.items.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import ru.imaginaerum.wd.common.sounds.CustomSoundEvents;

public class DungeonMasterCheese extends Item {
    public static final int MAX_BITES = 2;
    public static final String CHEESE_STATE = "cheese_state";

    public DungeonMasterCheese(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(20) // Восстановление 20 единиц голода
                .saturationMod(1.0f) // Насыщенность 1.0 (можно настроить)
                .effect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0), 1F) // Эффект регенерации на 8 секунд
                .build())); // Применяем все параметры еды
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return CustomSoundEvents.NYAMNYAM.get();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack itemStack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        CompoundTag tag = stack.getOrCreateTag();
        int bites = tag.getInt(CHEESE_STATE);

        world.playSound(null, entity, CustomSoundEvents.NYAMNYAM_END.get(),
                SoundSource.AMBIENT, 1.0F, 1.0F);

        // Восстановление голода и эффект
        if (bites < MAX_BITES) {
            if (entity instanceof Player player) {
                player.getFoodData().eat(20, 1.0F);  // Восстановление 20 единиц голода
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0)); // Эффект регенерации на 8 секунд
            }

            // Создаем новый стак с новым состоянием
            ItemStack newStack = new ItemStack(this, 1);
            CompoundTag newTag = newStack.getOrCreateTag();
            newTag.putInt(CHEESE_STATE, bites + 1);
            newStack.getOrCreateTag().putInt("CustomModelData", bites + 1);

            // Уменьшаем исходный стак
            stack.shrink(1);

            // Если это игрок, добавляем новый стак в инвентарь
            if (entity instanceof Player player) {
                if (!player.getInventory().add(newStack)) {
                    player.drop(newStack, false); // Если не удалось добавить в инвентарь, выбрасываем предмет
                }
            }
        } else {
            // При максимальном числе укусов просто удаляем предмет
            stack.shrink(1);
            if (entity instanceof Player player) {
                player.getFoodData().eat(20, 1.0F);  // Восстановление 20 единиц голода
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0)); // Эффект регенерации на 8 секунд
            }
        }

        return super.finishUsingItem(stack, world, entity);
    }
}