package ru.imaginaerum.wd.common.entities;


import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.entities.item_projectile_entities.*;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WD.MODID);

    public static final RegistryObject<EntityType<StarBall>> STAR_BALL = register("projectile_star_ball",
            EntityType.Builder.<StarBall>of(StarBall::new, MobCategory.MISC).setCustomClientFactory(StarBall::new)
                    .setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));

    public static final RegistryObject<EntityType<MilkBottle>> MILK_BOTTLE = register("projectile_milk_bottle",
            EntityType.Builder.<MilkBottle>of(MilkBottle::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).sized(0.5f, 0.5f));
    public static final RegistryObject<EntityType<CocktailMolokov>> COCKTAIL_MOLOKOV = register("projectile_cocktail_molokov",
            EntityType.Builder.<CocktailMolokov>of(CocktailMolokov::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).sized(0.5f, 0.5f));
    public static final RegistryObject<EntityType<WitheringCocktailMolokov>> WITHERING_COCKTAIL_MOLOKOV = register("projectile_withering_cocktail_molokov",
            EntityType.Builder.<WitheringCocktailMolokov>of(WitheringCocktailMolokov::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).sized(0.5f, 0.5f));
    public static final RegistryObject<EntityType<DisorientingCocktailMolokov>> DISORIENTING_COCKTAIL_MOLOKOV = register("projectile_disorienting_cocktail_molokov",
            EntityType.Builder.<DisorientingCocktailMolokov>of(DisorientingCocktailMolokov::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).sized(0.5f, 0.5f));
    public static final RegistryObject<EntityType<SpicyCocktailMolokov>> SPICY_COCKTAIL_MOLOKOV = register("projectile_spicy_cocktail_molokov",
            EntityType.Builder.<SpicyCocktailMolokov>of(SpicyCocktailMolokov::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).sized(0.5f, 0.5f));


    private static <T extends Entity> RegistryObject<EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
        return ENTITY_TYPES.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
    }
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}