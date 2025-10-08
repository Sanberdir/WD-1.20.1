package ru.imaginaerum.wd.common.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.client.gui.ars_melima.NetworkCookingXp;
import ru.imaginaerum.wd.common.blocks.entity.ModBlockEntities;
import ru.imaginaerum.wd.common.blocks.entity.renderer.DragoliteCageEntityRenderer;
import ru.imaginaerum.wd.common.blocks.entity.renderer.EchotronRenderer;
import ru.imaginaerum.wd.common.blocks.entity.renderer.GlowingJamBlockRenderer;
import ru.imaginaerum.wd.common.items.ItemsWD;
import ru.imaginaerum.wd.common.items.custom.SoulStone;
import ru.imaginaerum.wd.common.items.entity.client.ModModelLayersItem;
import ru.imaginaerum.wd.common.particles.ModParticles;
import ru.imaginaerum.wd.common.particles.custom.*;


@Mod.EventBusSubscriber(modid = WD.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventClientBusEvents {


    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSideHandler {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Вызвать на КЛИЕНТСКОЙ стороне
        }

        @SubscribeEvent
        public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {
            Minecraft.getInstance().particleEngine.register(ModParticles.ROBIN_STAR_PARTICLES.get(),
                    RobinStarsParticles.Provider::new);

            Minecraft.getInstance().particleEngine.register(ModParticles.ROBIN_STAR_PARTICLES_PROJECTILE.get(),
                    RobinStarsParticlesProjectile.Provider::new);
            Minecraft.getInstance().particleEngine.register(ModParticles.GOLDEN_FLOWER_PARTICLES.get(),
                    GoldenFlowerParticle.Provider::new);

            Minecraft.getInstance().particleEngine.register(ModParticles.STOMBLE_ROSE.get(),
                    StombleRoseParticles.Provider::new);
            Minecraft.getInstance().particleEngine.register(ModParticles.FLIES.get(),
                    FliesParticles.Provider::new);
        }
    }
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemProperties.register(ItemsWD.SOUL_STONE.get(), new ResourceLocation("charged"),
                (stack, world, entity, seed) -> stack.getItem() instanceof SoulStone && ((SoulStone) stack.getItem()).isCharged(new ItemStack(ItemsWD.SOUL_STONE.get())) ? 1.0F : 0.0F);

    }
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayersItem.APPLE_BOAT_LAYER, BoatModel::createBodyModel);
        event.registerLayerDefinition(ModModelLayersItem.APPLE_CHEST_BOAT_LAYER, ChestBoatModel::createBodyModel);
    }
    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DRAGOLITE_CAGE_ENTITY.get(), DragoliteCageEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.APPLE_SIGN.get(), SignRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GLOWING_JAM.get(), GlowingJamBlockRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ECHOTRON_ENTITY.get(), EchotronRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.APPLE_HANGING_SIGN.get(), HangingSignRenderer::new);
    }
}