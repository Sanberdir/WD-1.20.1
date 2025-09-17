package ru.imaginaerum.wd.common.items.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BrightPepper extends Item {
    public BrightPepper(Properties properties) {
        super(properties);
    }
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // Добавляем эффект возгорания на 8 секунд
        if (!level.isClientSide) {
            entity.setSecondsOnFire(8);
        }
        // Уменьшаем количество предметов в стеке
        return super.finishUsingItem(stack, level, entity);
    }

}
