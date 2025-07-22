package ru.imaginaerum.wd;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import ru.imaginaerum.wd.client.ClientProxy;
import ru.imaginaerum.wd.common.armor.elytra.DragoliteElytraArmorStandLayer;
import ru.imaginaerum.wd.common.armor.elytra.DragoliteElytraLayer;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.blocks.entity.ModBlockEntities;
import ru.imaginaerum.wd.common.custom_recipes.ProperBrewingRecipe;
import ru.imaginaerum.wd.common.effects.EffectsWD;
import ru.imaginaerum.wd.common.entities.ModEntities;
import ru.imaginaerum.wd.common.items.ItemsWD;
import ru.imaginaerum.wd.common.items.armor.model_layered.WDModelLayers;
import ru.imaginaerum.wd.common.items.arrows.DispenserRegistry;
import ru.imaginaerum.wd.common.items.arrows.EntityTypeInit;
import ru.imaginaerum.wd.common.items.arrows.FlameArrowRenderer;
import ru.imaginaerum.wd.common.items.entity.ModEntitiesItem;
import ru.imaginaerum.wd.common.items.entity.client.ModBoatRenderer;
import ru.imaginaerum.wd.common.particles.ModParticles;
import ru.imaginaerum.wd.common.sounds.CustomSoundEvents;
import ru.imaginaerum.wd.common.tab.TabWD;
import ru.imaginaerum.wd.server.CommonProxy;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(WD.MODID)
public class WD {
    public static CommonProxy PROXY = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    public static final String MODID = "wd";
    // Directly reference a slf4j logger

    public WD() {

        PROXY.commonInit();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        if (FMLEnvironment.dist.isClient()) modEventBus.addListener(this::registerElytraLayer);
        modEventBus.addListener(this::registerLayerDefinitions);
        EntityTypeInit.ENTITY_TYPES.register(modEventBus);
        ItemsWD.ITEMS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        BlocksWD.BLOCKS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        CustomSoundEvents.SOUND_EVENTS.register(modEventBus);
        EffectsWD.MOB_EFFECTS.register(modEventBus);
        ModEntitiesItem.ENTITIES.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        TabWD.CREATIVE_MODE_TABS.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        FMLJavaModLoadingContext.get().getModEventBus().addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == TabWD.WD_TAB.getKey()) {

            }
        });
    }
    private void registerLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        WDModelLayers.register(event);
    }
    public static ItemStack createPotion(Potion potion) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
    }
    public static ItemStack createSplashPotion(Potion potion) {
        return PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), potion);
    }
    public static ItemStack createLingeringPotion(Potion potion) {
        return PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), potion);
    }
    public static ItemStack createItemStack(Item item) {
        return new ItemStack(item);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

        event.enqueueWork(() -> {
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(Potions.WATER)), Ingredient.of(ItemsWD.WARPED_WART.get()), createPotion(Potions.AWKWARD)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createSplashPotion(Potions.WATER)), Ingredient.of(ItemsWD.WARPED_WART.get()), createSplashPotion(Potions.AWKWARD)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createLingeringPotion(Potions.WATER)), Ingredient.of(ItemsWD.WARPED_WART.get()), createLingeringPotion(Potions.AWKWARD)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.SUGAR_REFINED.get()), createPotion(Potions.STRONG_SWIFTNESS)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createSplashPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.SUGAR_REFINED.get()), createSplashPotion(Potions.STRONG_SWIFTNESS)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createLingeringPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.SUGAR_REFINED.get()), createLingeringPotion(Potions.STRONG_SWIFTNESS)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.HANDFUL_NETHER.get()), createPotion(Potions.STRONG_STRENGTH)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createSplashPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.HANDFUL_NETHER.get()), createSplashPotion(Potions.STRONG_STRENGTH)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createLingeringPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.HANDFUL_NETHER.get()), createLingeringPotion(Potions.STRONG_STRENGTH)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.PEPPER.get()), createPotion(Potions.LONG_FIRE_RESISTANCE)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createSplashPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.PEPPER.get()), createSplashPotion(Potions.LONG_FIRE_RESISTANCE)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createLingeringPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.PEPPER.get()), createLingeringPotion(Potions.LONG_FIRE_RESISTANCE)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.POISON_BERRY.get()), createPotion(Potions.STRONG_POISON)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createSplashPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.POISON_BERRY.get()), createSplashPotion(Potions.STRONG_POISON)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createLingeringPotion(Potions.AWKWARD)), Ingredient.of(ItemsWD.POISON_BERRY.get()), createLingeringPotion(Potions.STRONG_POISON)));
            BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(Ingredient.of(createItemStack(Items.GLASS_BOTTLE)), Ingredient.of(ItemsWD.COASTAL_STEEP_FLOWER.get()), createPotion(Potions.WATER)));
        });
        DispenserRegistry.registerBehaviors();
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
//        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
//            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @OnlyIn(Dist.CLIENT)
    private void registerElytraLayer(EntityRenderersEvent event) {
        if (event instanceof EntityRenderersEvent.AddLayers addLayersEvent) {
            EntityModelSet entityModels = addLayersEvent.getEntityModels();
            addLayersEvent.getSkins().forEach(s -> {
                LivingEntityRenderer<? extends Player, ? extends EntityModel<? extends Player>> livingEntityRenderer = addLayersEvent.getSkin(s);
                if (livingEntityRenderer instanceof PlayerRenderer playerRenderer) {
                    playerRenderer.addLayer(new DragoliteElytraLayer(playerRenderer, entityModels));
                }
            });
            LivingEntityRenderer<ArmorStand, ? extends EntityModel<ArmorStand>> livingEntityRenderer = addLayersEvent.getRenderer(EntityType.ARMOR_STAND);
            if (livingEntityRenderer instanceof ArmorStandRenderer armorStandRenderer) {
                armorStandRenderer.addLayer(new DragoliteElytraArmorStandLayer(armorStandRenderer, entityModels));
            }

        }
    }

    private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

    public static void queueServerWork(int tick, Runnable action) {
        workQueue.add(new AbstractMap.SimpleEntry(action, tick));
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
            workQueue.forEach(work -> {
                work.setValue(work.getValue() - 1);
                if (work.getValue() == 0)
                    actions.add(work);
            });
            actions.forEach(e -> e.getKey().run());
            workQueue.removeAll(actions);
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> PROXY.clientInit());
            EntityRenderers.register(EntityTypeInit.FLAME_ARROW.get(), FlameArrowRenderer::new);
            EntityRenderers.register(ModEntitiesItem.MOD_BOAT.get(), pContext -> new ModBoatRenderer(pContext, false));
            EntityRenderers.register(ModEntitiesItem.MOD_CHEST_BOAT.get(), pContext -> new ModBoatRenderer(pContext, true));
            event.enqueueWork(() -> {

                ComposterBlock.COMPOSTABLES.put(ItemsWD.FIRE_STEM.get(), 0.2f);
                ComposterBlock.COMPOSTABLES.put(ItemsWD.WARPED_WART.get(), 0.2f);
                ComposterBlock.COMPOSTABLES.put(ItemsWD.POISON_BERRY.get(), 0.2f);
                ComposterBlock.COMPOSTABLES.put(ItemsWD.MEADOW_GOLDEN_FLOWER.get(), 0.2f);
                ComposterBlock.COMPOSTABLES.put(ItemsWD.ROSE_OF_GHOSTY_TEARS.get(), 0.2f);
                ComposterBlock.COMPOSTABLES.put(ItemsWD.ROSE_OF_THE_MURDERER.get(), 0.2f);
                ComposterBlock.COMPOSTABLES.put(ItemsWD.MEDICAL_POTATO.get(), 0.2f);
                ComposterBlock.COMPOSTABLES.put(ItemsWD.SPATIAL_ORCHID.get(), 0.2f);
                ComposterBlock.COMPOSTABLES.put(ItemsWD.APPLE_LEAVES.get(), 0.2f);
                ComposterBlock.COMPOSTABLES.put(ItemsWD.APPLE_LEAVES_STAGES.get(), 0.2f);
                ComposterBlock.COMPOSTABLES.put(ItemsWD.APPLE_SAPLING.get(), 0.2f);


            });
        }
    }
}
