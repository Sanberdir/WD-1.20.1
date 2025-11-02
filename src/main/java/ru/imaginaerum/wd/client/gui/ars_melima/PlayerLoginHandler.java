package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// В существующий класс или создать новый:
public class PlayerLoginHandler {

    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Синхронизируем весь прогресс задач при входе
            syncAllTaskProgress(serverPlayer);
        }
    }

    private static void syncAllTaskProgress(ServerPlayer player) {
        Map<String, Map<String, Integer>> allProgress = new HashMap<>();

        // Собираем прогресс по всем главам и задачам
        for (String chapterId : TaskManager.getLoadedChapterIds()) {
            List<Task> tasks = TaskManager.getTasksForChapter(player.server, chapterId);
            Map<String, Integer> chapterProgress = new HashMap<>();

            for (Task task : tasks) {
                int progress = ServerTaskStorage.getProgress(player, task.getId());
                chapterProgress.put(task.getId(), progress);
            }

            allProgress.put(chapterId, chapterProgress);
        }

        // Отправляем на клиент
        NetworkCookingXp.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new SyncAllTaskProgressPacket(allProgress)
        );

        System.out.println("[ArsMelima] Synced all task progress to player: " + allProgress.size() + " chapters");
    }
}