package ru.imaginaerum.wd.client.gui.ars_melima.screens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.wd.client.gui.ars_melima.NetworkCookingXp;
import ru.imaginaerum.wd.client.gui.ars_melima.SyncCookingXpPacket;

public class CookingXPManager {
    private static final String TAG_COOK_XP = "wd:cooking_xp";
    private static final String TAG_COOK_LEVEL = "wd:cooking_level";
    private static final int BASE_MAX = 30; // полный бар на уровне 0
    private static final double LEVEL_MULT = 1.5;

    // Получить XP игрока
    public static int getXp(Player player) {
        if (player == null) return 0;
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return data.getInt(TAG_COOK_XP);
    }
    public static void resetXp(Player player) {
        if (player == null) return;

        // Сбрасываем данные ТОЛЬКО через правильный метод
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        data.putInt(TAG_COOK_XP, 0);
        data.putInt(TAG_COOK_LEVEL, 0);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);

        System.out.println("[DEBUG] CookingXPManager.resetXp called");
    }
    // Получить уровень игрока
    public static int getLevel(Player player) {
        if (player == null) return 0;
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return data.getInt(TAG_COOK_LEVEL);
    }

    // Максимум XP для уровня
    public static int getMaxForLevel(int level) {
        double value = BASE_MAX * Math.pow(LEVEL_MULT, Math.max(0, level));
        return Math.max(1, (int) Math.floor(value));
    }

    // Установить XP игроку
    public static void setXp(Player player, int xp) {
        if (player == null) return;
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        data.putInt(TAG_COOK_XP, xp);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
    }

    // Установить уровень игроку
    public static void setLevel(Player player, int level) {
        if (player == null) return;
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        data.putInt(TAG_COOK_LEVEL, level);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
    }

    /**
     * Добавляет кулинарный XP игроку. Вызывать на сервере.
     * При накоплении >= max — уровень повышается, лишние очки остаются.
     */
    public static void addXp(Player player, int amount) {
        if (player == null || player.level().isClientSide) return;

        int xp = getXp(player) + amount;
        int level = getLevel(player);

        int maxXp = getMaxForLevel(level);

        // Поднимаем уровень, если XP больше максимума
        while (xp >= maxXp) {
            xp -= maxXp;
            level++;
            maxXp = getMaxForLevel(level);
        }

        // Сохраняем на сервере
        setXp(player, xp);
        setLevel(player, level);

        // Отправляем клиенту, чтобы GUI обновился
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkCookingXp.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncCookingXpPacket(xp, level)
            );
        }
    }
}