package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.RequestUnlockProgressPacket;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.SyncUnlockedProgressPacket;

public class NetworkCookingXp {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new net.minecraft.resources.ResourceLocation("wd", "channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        // Server → Client: синхронизация XP
        CHANNEL.registerMessage(id++, SyncCookingXpPacket.class,
                SyncCookingXpPacket::encode,
                SyncCookingXpPacket::decode,
                SyncCookingXpPacket::handle);

        // Server → Client: синхронизация разблокированных прогрессий
        CHANNEL.registerMessage(id++, SyncUnlockedProgressPacket.class,
                SyncUnlockedProgressPacket::encode,
                SyncUnlockedProgressPacket::decode,
                SyncUnlockedProgressPacket::handle);

        // Client → Server: запрос на разблокировку прогрессии
        CHANNEL.registerMessage(id++, RequestUnlockProgressPacket.class,
                RequestUnlockProgressPacket::encode,
                RequestUnlockProgressPacket::decode,
                RequestUnlockProgressPacket::handle);

        // === ДОБАВЛЯЕМ ПАКЕТЫ ДЛЯ СИСТЕМЫ ЗАДАЧ ===

        // Server → Client: синхронизация прогресса одной задачи
        CHANNEL.registerMessage(id++, SyncTaskProgressPacket.class,
                SyncTaskProgressPacket::encode,
                SyncTaskProgressPacket::decode,
                SyncTaskProgressPacket::handle);

        // Server → Client: синхронизация всего прогресса задач при входе
        CHANNEL.registerMessage(id++, SyncAllTaskProgressPacket.class,
                SyncAllTaskProgressPacket::encode,
                SyncAllTaskProgressPacket::decode,
                SyncAllTaskProgressPacket::handle);

        // Можно также добавить пакет для запроса обновления прогресса с клиента на сервер, если нужно
        // CHANNEL.registerMessage(id++, RequestTaskUpdatePacket.class,
        //         RequestTaskUpdatePacket::encode,
        //         RequestTaskUpdatePacket::decode,
        //         RequestTaskUpdatePacket::handle);
    }
}