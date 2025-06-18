package ru.imaginaerum.wd.common.trades;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.common.items.ItemsWD;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModTrades {
    @SubscribeEvent
    public static void registerWanderingTrades(WandererTradesEvent event) {
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                new ItemStack(ItemsWD.SILVERAN.get(),2), 20, 10, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                        new ItemStack(ItemsWD.SILVERAN.get(),6), 20, 10, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                        new ItemStack(ItemsWD.SILVERAN.get(),4), 20, 10, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 3),
                        new ItemStack(ItemsWD.SILVERAN.get(),12), 20, 10, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                        new ItemStack(ItemsWD.HANDFUL_NETHER.get(),6), 20, 10, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 2),
                        new ItemStack(ItemsWD.HANDFUL_NETHER.get(),10), 20, 10, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                        new ItemStack(ItemsWD.HANDFUL_YADOGA.get(),6), 20, 10, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 2),
                        new ItemStack(ItemsWD.HANDFUL_YADOGA.get(),10), 20, 10, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(ItemsWD.HANDFUL_NETHER.get(), 5),
                        new ItemStack(Items.EMERALD,1), 20, 10, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(ItemsWD.HANDFUL_NETHER.get(), 8),
                        new ItemStack(Items.EMERALD,1), 20, 10, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 1),
                        new ItemStack(ItemsWD.HANDFUL_NETHER.get(),5), 20, 10, 0f));



        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 35),
                        new ItemStack(ItemsWD.MAGIC_HAT.get()), 5, 30, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 28),
                        new ItemStack(ItemsWD.MAGIC_HAT.get()), 5, 30, 0f));
        event.getGenericTrades()
                .add(new BasicItemListing(new ItemStack(Items.EMERALD, 31),
                        new ItemStack(ItemsWD.MAGIC_HAT.get()), 5, 30, 0f));

    }
}