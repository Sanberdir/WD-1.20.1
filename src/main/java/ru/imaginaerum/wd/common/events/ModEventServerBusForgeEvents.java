package ru.imaginaerum.wd.common.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.client.gui.ars_melima.ModNetwork;
import ru.imaginaerum.wd.client.gui.ars_melima.SyncCookingXpPacket;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.CookingXPManager;

@Mod.EventBusSubscriber(modid = WD.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEventServerBusForgeEvents {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        CookingXPManager.resetXp(serverPlayer);

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new SyncCookingXpPacket(0, 0)
        );

    }
}