package ru.imaginaerum.wd.common.tab;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.blocks.BlocksWD;
import ru.imaginaerum.wd.common.items.ItemsWD;

public class TabWD extends CreativeModeTab {
    protected TabWD(Builder builder) {
        super(builder);
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WD.MODID);

    public static final RegistryObject<CreativeModeTab> WD_TAB = CREATIVE_MODE_TABS.register("wd_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ItemsWD.JAM_TONIC.get()))
                    .title(Component.translatable("creativetab.wd"))
                    .displayItems((pParameters, output) -> {
                        //Items
                        output.accept(ItemsWD.IRON_WATERING_CAN.get());
                        output.accept(ItemsWD.MAGIC_HAT.get());
                        output.accept(ItemsWD.MAGIC_HAT_JAM.get());
                        output.accept(ItemsWD.POISON_BERRY_JAM.get());
                        output.accept(ItemsWD.SWEET_JAM.get());
                        output.accept(ItemsWD.SUGAR_REFINED.get());
                        output.accept(ItemsWD.LEVITAN_JAM.get());
                        output.accept(ItemsWD.APPLE_JAM.get());
                        output.accept(ItemsWD.JAM_INVISIBILITY.get());
                        output.accept(ItemsWD.JAM_TONIC.get());
                        output.accept(ItemsWD.CHARMING_JAM.get());
                        output.accept(ItemsWD.GLOWING_JAM.get());
                        output.accept(ItemsWD.FREEZE_JAM.get());
                        output.accept(ItemsWD.JAR.get());
                        output.accept(ItemsWD.ARS_MELIMA.get());
                        output.accept(ItemsWD.RAW_BEAR_MEAT.get());
                        output.accept(ItemsWD.COOKED_BEAR_MEAT.get());
                        output.accept(ItemsWD.RAW_HORSE.get());
                        output.accept(ItemsWD.COOKED_HORSE.get());
                        output.accept(ItemsWD.RAW_GOATS_MEAT.get());
                        output.accept(ItemsWD.GOAT_MEAT_KEBAB.get());
                        output.accept(ItemsWD.COOKED_GOAT_MEAT_KEBAB.get());
                        output.accept(ItemsWD.CAMEL_MEAT_KEBAB.get());
                        output.accept(ItemsWD.COOKED_CAMEL_MEAT_KEBAB.get());
                        output.accept(ItemsWD.RAW_CAMEL_MEAT.get());
                        output.accept(ItemsWD.COOKED_CAMEL_MEAT.get());
                        output.accept(ItemsWD.RAW_SLICING_CAMEL_MEAT.get());
                        output.accept(ItemsWD.COOKED_SLICING_CAMEL_MEAT.get());
                        output.accept(ItemsWD.COOKED_GOATS_MEAT.get());
                        output.accept(ItemsWD.GOATS_MEAT_PILAF.get());
                        output.accept(ItemsWD.FROG_BODY.get());
                        output.accept(ItemsWD.COOKED_FROG.get());
                        output.accept(ItemsWD.FROG_LEGS.get());
                        output.accept(ItemsWD.COOKED_FROG_LEGS.get());
                        output.accept(ItemsWD.SPAGETTI_IN_THE_NORTH.get());
                        output.accept(ItemsWD.SWEET_ROLL.get());
                        output.accept(ItemsWD.GOULASH_WITH_GOAT_MEAT.get());
                        output.accept(ItemsWD.ROAST_GOAT_MEAT_WITH_FREEZE_BERRIES_SYRUP.get());
                        output.accept(ItemsWD.BEAR_MEAT_SOUP.get());
                        output.accept(ItemsWD.MEDICAL_POTATO.get());
                        output.accept(ItemsWD.BRIGHT_PEPPER.get());
                        output.accept(ItemsWD.KRUTNEVY_BREAD.get());
                        output.accept(ItemsWD.TURTLE_SOUP.get());
                        output.accept(ItemsWD.CLEANED_TURTLE_NECK.get());
                        output.accept(ItemsWD.PICKLED_TURTLE_NECK.get());
                        output.accept(ItemsWD.TURTLE_NECK.get());
                        output.accept(ItemsWD.HOT_COCOA_WITH_SPARKING_POLLEN.get());
                        output.accept(ItemsWD.HUNTING_TWISTER.get());
                        output.accept(ItemsWD.IRIS.get());
                        output.accept(ItemsWD.MUSHROOM_ON_STICK.get());
                        output.accept(ItemsWD.COOKED_MUSHROOM_ON_STICK.get());
                        output.accept(ItemsWD.SHPIKACHKI.get());
                        output.accept(ItemsWD.COOKED_SHPIKACHKI.get());
                        output.accept(ItemsWD.DUNGEON_MASTER_CHEESE.get());
                        output.accept(ItemsWD.SILVERAN.get());
                        output.accept(ItemsWD.HANDFUL_YADOGA.get());
                        output.accept(ItemsWD.A_DROP_OF_LOVE.get());
                        output.accept(ItemsWD.CLEANSING_DECOCTION.get());
                        output.accept(ItemsWD.HANDFUL_NETHER.get());
                        output.accept(ItemsWD.SPARKLING_POLLEN.get());
                        output.accept(ItemsWD.MUSIC_DISK_1.get());
                        output.accept(ItemsWD.MUSIC_DISK_2.get());
                        output.accept(ItemsWD.MUSIC_DISK_3.get());
                        output.accept(ItemsWD.CRIMSON_BONE_MEAL.get());
                        output.accept(ItemsWD.WARPED_BONE_MEAL.get());
                        output.accept(ItemsWD.GRASS_BONE_MEAL.get());
                        output.accept(ItemsWD.MYCELIUM_BONE_MEAL.get());
                        output.accept(ItemsWD.COOKED_SLICING_GOATS_MEAT.get());
                        output.accept(ItemsWD.RAW_SLICING_GOATS_MEAT.get());
                        output.accept(ItemsWD.COASTAL_STEEP_FIBERS.get());
                        output.accept(ItemsWD.COASTAL_STEEP_FLOWER.get());
                        output.accept(ItemsWD.FLAME_ARROW.get());
                        output.accept(ItemsWD.CHARMING_BERRIES.get());
                        output.accept(ItemsWD.SOUL_STONE.get());
                        output.accept(ItemsWD.ROSE_OF_THE_MURDERER.get());
                        output.accept(ItemsWD.ROTTEN_PIE.get());
                        output.accept(ItemsWD.WIZARD_PIE.get());
                        output.accept(ItemsWD.WIZARD_PIE_SLICE.get());
                        output.accept(ItemsWD.ROTTEN_PIE_SLICE.get());
                        output.accept(ItemsWD.ROBIN_STICK.get());
                        output.accept(ItemsWD.DRAGOLIT_INGOT.get());
                        output.accept(ItemsWD.STRANGE_SCRAP.get());
                        output.accept(ItemsWD.CLEAR_DRAGOLIT_NUGGET.get());
                        output.accept(ItemsWD.DRAGOLIT_RAPIER.get());
                        output.accept(ItemsWD.HEALING_DEW.get());
                        output.accept(ItemsWD.NETHER_GROG.get());
                        output.accept(ItemsWD.SPATIAL_ORCHID.get());
                        output.accept(ItemsWD.COASTAL_STEEP.get());
                        output.accept(ItemsWD.GOLDEN_CHEST_KING_PILLAGER.get());
                        output.accept(ItemsWD.THE_PILLAGERS_KEY.get());
                        output.accept(ItemsWD.THE_KING_PILLAGERS_KEY.get());
                        output.accept(ItemsWD.MAG_ELYTRA.get());
                        output.accept(ItemsWD.DRAGOLITE_UPGRADE_SMITHING_TEMPLATE.get());
                        output.accept(ItemsWD.MEADOW_GOLDEN_FLOWER.get());
                        output.accept(ItemsWD.APPLE_SAPLING.get());
                        output.accept(ItemsWD.APPLE_LEAVES.get());
                        output.accept(ItemsWD.APPLE_LEAVES_STAGES.get());
                        output.accept(ItemsWD.APPLE_SIGN.get());
                        output.accept(ItemsWD.APPLE_HANGING_SIGN.get());
                        output.accept(ItemsWD.APPLE_BOAT.get());
                        output.accept(ItemsWD.APPLE_CHEST_BOAT.get());
                        output.accept(ItemsWD.WAFFLES.get());
                        output.accept(ItemsWD.BERRIES_WAFFLES.get());
                        output.accept(ItemsWD.APPLE_WAFFLES.get());
                        output.accept(ItemsWD.ICE_WAFFLES.get());
                        output.accept(ItemsWD.RAW_WAFFLES.get());
                        output.accept(ItemsWD.POISON_WAFFLES.get());
                        output.accept(ItemsWD.CHARMING_WAFFLES.get());
                        output.accept(ItemsWD.GLOW_BERRIES_WAFFLES.get());
                        output.accept(ItemsWD.BRIGHT_PEPPER_SEEDS.get());
                        //Blocks
                        //Meats_pots
                        output.accept(BlocksWD.POT.get());
                        output.accept(BlocksWD.POT_FROM_MEAT_GOAT.get());
                        output.accept(BlocksWD.POT_FROM_MEAT_CAMEL.get());
                        output.accept(BlocksWD.MARINADED_POT_FROM_MEAT_GOAT.get());
                        output.accept(BlocksWD.MARINADED_POT_FROM_MEAT_CAMEL.get());

                        output.accept(BlocksWD.ECHOTRON.get());
                        output.accept(BlocksWD.FIRE_STEM.get());
                        output.accept(BlocksWD.POISON_BERRY.get());
                        output.accept(BlocksWD.FREEZE_BERRIES.get());
                        output.accept(BlocksWD.WARPED_WART.get());
                        output.accept(BlocksWD.SUGAR_SACK.get());
                        output.accept(BlocksWD.THE_PILLAGERS_CHEST.get());
                        output.accept(BlocksWD.A_BLOCK_OF_SPARKING_POLLEN.get());
                        output.accept(BlocksWD.ROSE_OF_GHOSTY_TEARS.get());
                        output.accept(BlocksWD.DRAGOLIT_GRID.get());
                        output.accept(BlocksWD.DRAGOLIT_BLOCK.get());
                        output.accept(ItemsWD.DRAGOLITE_CAGE.get());
                        output.accept(BlocksWD.STRANGE_CHIP.get());
                        output.accept(BlocksWD.APPLE_LOG.get());
                        output.accept(BlocksWD.APPLE_WOOD.get());
                        output.accept(BlocksWD.STRIPPED_APPLE_LOG.get());
                        output.accept(BlocksWD.STRIPPED_APPLE_WOOD.get());
                        output.accept(BlocksWD.APPLE_PLANKS.get());
                        output.accept(BlocksWD.APPLE_CABINET.get());
                        output.accept(BlocksWD.ROTTEN_PIE_CAGE.get());
                        output.accept(BlocksWD.MAGIC_COMPOST.get());
                        output.accept(BlocksWD.MAGIC_SOIL.get());
                        output.accept(BlocksWD.MAGIC_SOIL_GRASS.get());
                        output.accept(BlocksWD.MAGIC_SOIL_FARMLAND.get());
                        output.accept(BlocksWD.APPLE_STAIRS.get());
                        output.accept(BlocksWD.APPLE_SLAB.get());
                        output.accept(BlocksWD.APPLE_FENCE.get());
                        output.accept(BlocksWD.APPLE_FENCE_GATE.get());
                        output.accept(BlocksWD.APPLE_BUTTON.get());
                        output.accept(BlocksWD.APPLE_PRESSURE_PLATE.get());
                        output.accept(BlocksWD.APPLE_DOOR.get());
                        output.accept(BlocksWD.APPLE_TRAPDOOR.get());

                    })
                    .build());

}
