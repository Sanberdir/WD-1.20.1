package ru.imaginaerum.wd.common.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.blocks.ModFlammableBlocks;

@Mod.EventBusSubscriber(modid = WD.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventSubscriber {
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModFlammableBlocks::registerFlammableBlocks);
    }

}