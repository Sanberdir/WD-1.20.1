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
        System.out.println("[ArsMelima] SyncTaskProgressPacket created: " + learningChapterId + "/" + taskId + " = " + progress);
    }

    public static void encode(SyncTaskProgressPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.learningChapterId);
        buffer.writeUtf(packet.taskId);
        buffer.writeInt(packet.progress);
        System.out.println("[ArsMelima] Encoding SyncTaskProgressPacket: " + packet.learningChapterId + "/" + packet.taskId + " = " + packet.progress);
    }

    public static SyncTaskProgressPacket decode(FriendlyByteBuf buffer) {
        String chapterId = buffer.readUtf();
        String taskId = buffer.readUtf();
        int progress = buffer.readInt();
        System.out.println("[ArsMelima] Decoding SyncTaskProgressPacket: " + chapterId + "/" + taskId + " = " + progress);
        return new SyncTaskProgressPacket(chapterId, taskId, progress);
    }

    public static void handle(SyncTaskProgressPacket packet, Supplier<NetworkEvent.Context> context) {
        System.out.println("[ArsMelima] Handling SyncTaskProgressPacket on CLIENT: " + packet.learningChapterId + "/" + packet.taskId + " = " + packet.progress);

        context.get().enqueueWork(() -> {
            ClientTaskData.updateTaskProgress(packet.learningChapterId, packet.taskId, packet.progress);
        });
        context.get().setPacketHandled(true);
    }
}