package ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.wd.client.gui.ars_melima.NetworkCookingXp;
import ru.imaginaerum.wd.client.gui.ars_melima.SyncCookingXpPacket;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressionLoader;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.CookingXPManager;

import java.util.List;
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
            if (player == null) return;

            List<String> unlocked = ProgressionUnlockManager.getUnlockedList(player);
            List<ProgressNode> nodes = ProgressionLoader.loadNodes();

            // --- Автоматически добавляем root-ноды (без parentId и locked=false) ---
            for (ProgressNode n : nodes) {
                if ((n.getParentId() == null || n.getParentId().isEmpty()) && !n.isLocked()) {
                    if (!unlocked.contains(n.getId())) {
                        ProgressionUnlockManager.unlock(player, n.getId());
                        unlocked.add(n.getId());
                        System.out.println("[ArsMelima] Auto-unlocked root node: " + n.getId());
                    }
                }
            }

            // --- Ищем текущую ноду ---
            ProgressNode currentNode = null;
            for (ProgressNode node : nodes) {
                if (node.getId().equals(nodeId)) {
                    currentNode = node;
                    break;
                }
            }

            if (currentNode == null) {
                System.out.println("[ArsMelima] Node not found: " + nodeId);
                return;
            }

            // --- Если нода уже разблокирована ---
            if (unlocked.contains(currentNode.getId())) {
                player.sendSystemMessage(Component.literal("§7Этот узел уже разблокирован."));
                return;
            }

            // --- Проверка родителя ---
            String parentId = currentNode.getParentId();
            if (parentId != null && !parentId.isEmpty()) {
                if (!unlocked.contains(parentId)) {
                    player.sendSystemMessage(Component.literal("§cСначала разблокируйте родительский узел: " + parentId));
                    return;
                }
            }

            // --- Проверка уровня ---
            int currentLevel = CookingXPManager.getLevel(player);
            if (currentLevel < 5) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SyncNotEnoughCookingLevelPacket());
                return;
            }

            // --- Всё ок: разблокируем ---
            CookingXPManager.setLevel(player, currentLevel - 5);
            ProgressionUnlockManager.unlock(player, currentNode.getId());

            // --- Синхронизация ---
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncCookingXpPacket(CookingXPManager.getXp(player), CookingXPManager.getLevel(player)));
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncUnlockedProgressPacket(ProgressionUnlockManager.getUnlockedList(player)));

            player.sendSystemMessage(Component.literal("§aРазблокирован узел: " + currentNode.getId()));
        });

        ctx.get().setPacketHandled(true);
        return true;
    }
}
