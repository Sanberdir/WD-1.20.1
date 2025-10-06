package ru.imaginaerum.wd.common.events;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.CookingXPManager;
import ru.imaginaerum.wd.common.blocks.ModFlammableBlocks;
import ru.imaginaerum.wd.common.blocks.custom.DragolitBlock;

@Mod.EventBusSubscriber(modid = WD.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventSubscriber {
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModFlammableBlocks::registerFlammableBlocks);
    }

}