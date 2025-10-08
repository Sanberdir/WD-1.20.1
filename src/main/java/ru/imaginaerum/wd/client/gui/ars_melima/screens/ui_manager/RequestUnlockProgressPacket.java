package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.wd.client.gui.ars_melima.NetworkCookingXp;
import ru.imaginaerum.wd.client.gui.ars_melima.SyncCookingXpPacket;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.CookingXPManager;

import java.util.function.Supplier;

import static ru.imaginaerum.wd.client.gui.ars_melima.NetworkCookingXp.CHANNEL;

public class RequestUnlockProgressPacket {
    private final String nodeId;

    public RequestUnlockProgressPacket(String nodeId) {
        this.nodeId = nodeId;
    }

    public static RequestUnlockProgressPacket decode(FriendlyByteBuf buf) {
        return new RequestUnlockProgressPacket(buf.readUtf(32767));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(nodeId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender(); // ServerPlayer
            if (player != null) {
                int currentLevel = CookingXPManager.getLevel(player);
                if (currentLevel >= 5) {
                    CookingXPManager.setLevel(player, currentLevel - 5);
                    ProgressionUnlockManager.unlock(player, nodeId);

                    // Синхронизация XP/Level
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new SyncCookingXpPacket(CookingXPManager.getXp(player), CookingXPManager.getLevel(player)));

                    // Синхронизация прогрессов
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new SyncUnlockedProgressPacket(ProgressionUnlockManager.getUnlockedList(player)));
                } else {
                    // Отправляем клиенту пакет для отображения текста в GUI
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new SyncNotEnoughCookingLevelPacket());
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
