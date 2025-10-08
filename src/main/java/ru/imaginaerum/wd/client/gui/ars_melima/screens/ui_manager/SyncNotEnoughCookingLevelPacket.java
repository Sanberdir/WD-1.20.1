package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static ru.imaginaerum.wd.client.gui.ars_melima.NetworkCookingXp.CHANNEL;

public class SyncNotEnoughCookingLevelPacket {
    public SyncNotEnoughCookingLevelPacket() {}

    public static SyncNotEnoughCookingLevelPacket decode(FriendlyByteBuf buf) {
        return new SyncNotEnoughCookingLevelPacket();
    }

    public void encode(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ru.imaginaerum.wd.client.gui.ars_melima.screens.ClientCookingData.showNotEnoughLevels = true;
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}