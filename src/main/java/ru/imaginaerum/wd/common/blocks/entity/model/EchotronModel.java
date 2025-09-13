package ru.imaginaerum.wd.common.blocks.entity.model;

import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.blocks.entity.EchotronBlockEntity;
import ru.imaginaerum.wd.common.blocks.entity.GlowingJamBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class EchotronModel extends GeoModel<EchotronBlockEntity> {
    @Override
    public ResourceLocation getModelResource(EchotronBlockEntity animatable) {
        return new ResourceLocation(WD.MODID, "geo/echotron.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EchotronBlockEntity animatable) {
        return new ResourceLocation(WD.MODID, "textures/block/echotron.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EchotronBlockEntity echotronBlockEntity) {
        return null;
    }

}
