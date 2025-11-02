package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncTaskProgressPacket {
    private final String learningChapterId;
    private final String taskId;
    private final int progress;

    public SyncTaskProgressPacket(String learningChapterId, String taskId, int progress) {
        this.learningChapterId = learningChapterId;
        this.taskId = taskId;
        this.progress = progress;
    }

    public static void encode(SyncTaskProgressPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.learningChapterId);
        buffer.writeUtf(packet.taskId);
        buffer.writeInt(packet.progress);
    }

    public static SyncTaskProgressPacket decode(FriendlyByteBuf buffer) {
        return new SyncTaskProgressPacket(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readInt()
        );
    }

    public static void handle(SyncTaskProgressPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ClientTaskData.updateTaskProgress(packet.learningChapterId, packet.taskId, packet.progress);
        });
        context.get().setPacketHandled(true);
    }
}