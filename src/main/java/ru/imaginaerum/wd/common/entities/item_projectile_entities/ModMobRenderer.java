package ru.imaginaerum.wd.common.entities.item_projectile_entities;


import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.common.entities.ModEntities;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)

public class ModMobRenderer {
    @SubscribeEvent
    public static void registerStarBallRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STAR_BALL.get(), ThrownItemRenderer::new);
    }
    @SubscribeEvent
    public static void registerMilkBottleRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MILK_BOTTLE.get(), ThrownItemRenderer::new);
    }
    @SubscribeEvent
    public static void registerCocktail_molokovRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COCKTAIL_MOLOKOV.get(), ThrownItemRenderer::new);
    }
    @SubscribeEvent
    public static void registerWitheringCocktail_molokovRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WITHERING_COCKTAIL_MOLOKOV.get(), ThrownItemRenderer::new);
    }
    @SubscribeEvent
    public static void registerDisorientingCocktail_molokovRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DISORIENTING_COCKTAIL_MOLOKOV.get(), ThrownItemRenderer::new);
    }
    @SubscribeEvent
    public static void registerSpicyCocktail_molokovRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SPICY_COCKTAIL_MOLOKOV.get(), ThrownItemRenderer::new);
    }

}