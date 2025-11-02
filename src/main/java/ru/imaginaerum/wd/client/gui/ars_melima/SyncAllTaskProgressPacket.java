package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncAllTaskProgressPacket {
    private final Map<String, Map<String, Integer>> allProgress;

    public SyncAllTaskProgressPacket(Map<String, Map<String, Integer>> allProgress) {
        this.allProgress = allProgress;
    }

    public static void encode(SyncAllTaskProgressPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.allProgress.size());
        for (Map.Entry<String, Map<String, Integer>> chapterEntry : packet.allProgress.entrySet()) {
            buffer.writeUtf(chapterEntry.getKey());
            Map<String, Integer> tasks = chapterEntry.getValue();
            buffer.writeInt(tasks.size());
            for (Map.Entry<String, Integer> taskEntry : tasks.entrySet()) {
                buffer.writeUtf(taskEntry.getKey());
                buffer.writeInt(taskEntry.getValue());
            }
        }
    }

    public static SyncAllTaskProgressPacket decode(FriendlyByteBuf buffer) {
        Map<String, Map<String, Integer>> progress = new HashMap<>();
        int chapterCount = buffer.readInt();
        for (int i = 0; i < chapterCount; i++) {
            String chapterId = buffer.readUtf();
            int taskCount = buffer.readInt();
            Map<String, Integer> tasks = new HashMap<>();
            for (int j = 0; j < taskCount; j++) {
                String taskId = buffer.readUtf();
                int taskProgress = buffer.readInt();
                tasks.put(taskId, taskProgress);
            }
            progress.put(chapterId, tasks);
        }
        return new SyncAllTaskProgressPacket(progress);
    }

    public static void handle(SyncAllTaskProgressPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ClientTaskData.initProgress(packet.allProgress);
            System.out.println("[ArsMelima] Received all task progress: " + packet.allProgress.size() + " chapters");
        });
        context.get().setPacketHandled(true);
    }
}