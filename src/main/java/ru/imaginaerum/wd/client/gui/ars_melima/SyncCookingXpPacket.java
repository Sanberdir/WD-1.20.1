package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ClientCookingData;

import java.util.function.Supplier;

public class SyncCookingXpPacket {
    private final int xp, level;

    public SyncCookingXpPacket(int xp, int level) {
        this.xp = xp;
        this.level = level;
    }

    public static SyncCookingXpPacket decode(FriendlyByteBuf buf) {
        return new SyncCookingXpPacket(buf.readInt(), buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(xp);
        buf.writeInt(level);
    }

    public static void handle(SyncCookingXpPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientCookingData.clientXp = packet.xp;
            ClientCookingData.clientLevel = packet.level;
        });
        ctx.get().setPacketHandled(true);
    }
}