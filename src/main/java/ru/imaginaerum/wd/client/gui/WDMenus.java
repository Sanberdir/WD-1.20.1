package ru.imaginaerum.wd.client.gui;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import ru.imaginaerum.wd.WD;

public class WDMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY_MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, WD.MODID);

}
