package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ProgressionUnlockManager {
    private static final String TAG_UNLOCKED = "wd:unlocked_progress";

    public static boolean isUnlocked(Player player, String id) {
        if (player == null || id == null) return false;
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ListTag list = data.getList(TAG_UNLOCKED, 8); // 8 = TAG_String
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.getString(i))) return true;
        }
        return false;
    }

    public static void unlock(Player player, String id) {
        if (player == null || id == null || id.isEmpty()) return;
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ListTag list = data.getList(TAG_UNLOCKED, 8);
        if (!isUnlocked(player, id)) {
            list.add(StringTag.valueOf(id));
            data.put(TAG_UNLOCKED, list);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
        }
    }

    public static List<String> getUnlockedList(Player player) {
        List<String> out = new ArrayList<>();
        if (player == null) return out;
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ListTag list = data.getList(TAG_UNLOCKED, 8);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }
}