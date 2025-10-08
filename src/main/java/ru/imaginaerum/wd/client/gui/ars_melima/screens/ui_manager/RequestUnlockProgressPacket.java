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
                // Проверяем, хватает ли 5 уровней
                int currentLevel = CookingXPManager.getLevel(player);
                if (currentLevel >= 5) {
                    // Списываем 5 уровней
                    CookingXPManager.setLevel(player, currentLevel - 5);

                    // Разблокируем прогресс-узел
                    ProgressionUnlockManager.unlock(player, nodeId);

                    // Синхронизация с клиентом: обновлённый XP и уровень
                    NetworkCookingXp.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new SyncCookingXpPacket(CookingXPManager.getXp(player), CookingXPManager.getLevel(player))
                    );

                    // Синхронизация с клиентом: список разблокированных прогрессий
                    NetworkCookingXp.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new SyncUnlockedProgressPacket(ProgressionUnlockManager.getUnlockedList(player))
                    );
                } else {
                    // Если мало уровней, можно вывести сообщение (на клиент или через системный чат)
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Недостаточно кулинарных уровней (нужно 5)"), true
                    );
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
