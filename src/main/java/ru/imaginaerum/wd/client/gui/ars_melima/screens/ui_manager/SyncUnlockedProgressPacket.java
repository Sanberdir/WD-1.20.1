package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class SyncUnlockedProgressPacket {
    private final java.util.List<String> unlocked;
    public SyncUnlockedProgressPacket(java.util.List<String> unlocked) { this.unlocked = unlocked; }
    public static SyncUnlockedProgressPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        java.util.List<String> list = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) list.add(buf.readUtf(32767));
        return new SyncUnlockedProgressPacket(list);
    }
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(unlocked.size());
        for (String s : unlocked) buf.writeUtf(s);
    }
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ru.imaginaerum.wd.client.gui.ars_melima.screens.ClientCookingData.unlockedProgress.clear();
            ru.imaginaerum.wd.client.gui.ars_melima.screens.ClientCookingData.unlockedProgress.addAll(unlocked);
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}