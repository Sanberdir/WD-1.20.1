package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ClientCookingData;

import java.util.function.Supplier;

public class SyncCookingXpPacket {
    private final int xp;
    private final int level;

    public SyncCookingXpPacket(int xp, int level) {
        this.xp = xp;
        this.level = level;
    }

    // Декодирование
    public static SyncCookingXpPacket decode(FriendlyByteBuf buf) {
        return new SyncCookingXpPacket(buf.readInt(), buf.readInt());
    }

    // Кодирование
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(xp);
        buf.writeInt(level);
    }

    // Обработка на клиенте
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Обновляем клиентские данные
            ClientCookingData.clientXp = xp;
            ClientCookingData.clientLevel = level;
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
