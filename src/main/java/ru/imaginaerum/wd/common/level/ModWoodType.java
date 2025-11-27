package ru.imaginaerum.wd.common.level;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import ru.imaginaerum.wd.WD;


public class ModWoodType {
    public static final WoodType APPLE_WOOD = WoodType.register(new WoodType(WD.MODID + ":apple_wood", BlockSetType.OAK));

}

