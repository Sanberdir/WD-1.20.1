package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ClientCookingData;

import java.util.function.Supplier;

public class SyncNotEnoughCookingLevelPacket {
    public SyncNotEnoughCookingLevelPacket() {}

    public static SyncNotEnoughCookingLevelPacket decode(FriendlyByteBuf buf) {
        return new SyncNotEnoughCookingLevelPacket();
    }

    public void encode(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientCookingData.showNotEnoughLevels = true;
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}