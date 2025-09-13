package ru.imaginaerum.wd.common.blocks.entity.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.blocks.entity.GlowingJamBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public class GlowingJamModel extends GeoModel<GlowingJamBlockEntity> {
    @Override
    public ResourceLocation getModelResource(GlowingJamBlockEntity animatable) {
        return new ResourceLocation(WD.MODID, "geo/glowing_jam.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GlowingJamBlockEntity animatable) {
        return new ResourceLocation(WD.MODID, "textures/block/glowing_jam.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GlowingJamBlockEntity glowingJamBlockEntity) {
        return null;
    }

}
