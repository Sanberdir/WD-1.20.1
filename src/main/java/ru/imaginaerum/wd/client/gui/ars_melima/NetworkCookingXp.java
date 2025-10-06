package ru.imaginaerum.wd.client.gui.ars_melima;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkCookingXp {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new net.minecraft.resources.ResourceLocation("wd", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void registerPackets() {
        CHANNEL.registerMessage(packetId++, SyncCookingXpPacket.class,
                SyncCookingXpPacket::encode,
                SyncCookingXpPacket::decode,
                SyncCookingXpPacket::handle);
    }
}
