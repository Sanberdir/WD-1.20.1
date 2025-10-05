package ru.imaginaerum.wd.common.items.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ru.imaginaerum.wd.client.gui.ars_melima.ArsMelimaScreen;

public class ArsMelima extends Item {
    public ArsMelima(Properties properties) {
        super(properties);
    }

    public static final String NBT_XP = "ars_melima_xp";
    public static final int MAX_XP = 6_000;

    public static int getStoredXp(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (!stack.hasTag()) return 0;
        return stack.getTag().getInt(NBT_XP);
    }

    public static void setStoredXp(ItemStack stack, int xp) {
        if (stack == null || stack.isEmpty()) return;
        int v = Math.max(0, Math.min(MAX_XP, xp));
        stack.getOrCreateTag().putInt(NBT_XP, v);
    }

    public static int addXpToStack(ItemStack stack, int amount) {
        if (stack == null || stack.isEmpty() || amount <= 0) return 0;
        int cur = getStoredXp(stack);
        int canAdd = Math.max(0, MAX_XP - cur);
        int toAdd = Math.min(canAdd, amount);
        if (toAdd > 0) {
            setStoredXp(stack, cur + toAdd);
        }
        return toAdd;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (level.isClientSide) {
            openScreen(held);
        }

        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private void openScreen(ItemStack stack) {
        Minecraft.getInstance().setScreen(new ArsMelimaScreen(stack));
    }
}
