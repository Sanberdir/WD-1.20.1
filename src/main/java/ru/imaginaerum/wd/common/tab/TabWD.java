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
                    .displayItems((pParameters, pOutput) -> {
                        //Items
                        pOutput.accept(ItemsWD.MAGIC_HAT.get());
                        pOutput.accept(ItemsWD.MAGIC_HAT_JAM.get());
                        pOutput.accept(ItemsWD.POISON_BERRY_JAM.get());
                        pOutput.accept(ItemsWD.SWEET_JAM.get());
                        pOutput.accept(ItemsWD.SUGAR_REFINED.get());
                        pOutput.accept(ItemsWD.LEVITAN_JAM.get());
                        pOutput.accept(ItemsWD.APPLE_JAM.get());
                        pOutput.accept(ItemsWD.JAM_INVISIBILITY.get());
                        pOutput.accept(ItemsWD.JAM_TONIC.get());
                        pOutput.accept(ItemsWD.CHARMING_JAM.get());
                        pOutput.accept(ItemsWD.GLOWING_JAM.get());
                        pOutput.accept(ItemsWD.FREEZE_JAM.get());
                        pOutput.accept(ItemsWD.JAR.get());
                        pOutput.accept(ItemsWD.RAW_BEAR_MEAT.get());
                        pOutput.accept(ItemsWD.RAW_HORSE.get());
                        pOutput.accept(ItemsWD.RAW_GOATS_MEAT.get());
                        pOutput.accept(ItemsWD.GOAT_MEAT_KEBAB.get());
                        pOutput.accept(ItemsWD.COCKED_GOAT_MEAT_KEBAB.get());
                        pOutput.accept(ItemsWD.COCKED_BEAR_MEAT.get());
                        pOutput.accept(ItemsWD.COCKED_HORSE.get());
                        pOutput.accept(ItemsWD.COCKED_GOATS_MEAT.get());
                        pOutput.accept(ItemsWD.FROG_BODY.get());
                        pOutput.accept(ItemsWD.COCKED_FROG.get());
                        pOutput.accept(ItemsWD.FROG_LEGS.get());
                        pOutput.accept(ItemsWD.COCKED_FROG_LEGS.get());
                        pOutput.accept(ItemsWD.SPAGETTI_IN_THE_NORTH.get());
                        pOutput.accept(ItemsWD.SWEET_ROLL.get());
                        pOutput.accept(ItemsWD.GOULASH_WITH_GOAT_MEAT.get());
                        pOutput.accept(ItemsWD.ROAST_GOAT_MEAT_WITH_FREEZE_BERRIES_SYRUP.get());
                        pOutput.accept(ItemsWD.BEAR_MEAT_SOUP.get());
                        pOutput.accept(ItemsWD.MEDICAL_POTATO.get());
                        pOutput.accept(ItemsWD.PEPPER.get());
                        pOutput.accept(ItemsWD.KRUTNEVY_BREAD.get());
                        pOutput.accept(ItemsWD.TURTLE_SOUP.get());
                        pOutput.accept(ItemsWD.CLEANED_TURTLE_NECK.get());
                        pOutput.accept(ItemsWD.PICKLED_TURTLE_NECK.get());
                        pOutput.accept(ItemsWD.TURTLE_NECK.get());
                        pOutput.accept(ItemsWD.HOT_COCOA_WITH_SPARKING_POLLEN.get());
                        pOutput.accept(ItemsWD.HUNTING_TWISTER.get());
                        pOutput.accept(ItemsWD.IRIS.get());
                        pOutput.accept(ItemsWD.MUSHROOM_ON_STICK.get());
                        pOutput.accept(ItemsWD.COCKED_MUSHROOM_ON_STICK.get());
                        pOutput.accept(ItemsWD.SHPIKACHKI.get());
                        pOutput.accept(ItemsWD.COCKED_SHPIKACHKI.get());
                        pOutput.accept(ItemsWD.CHEESE.get());
                        pOutput.accept(ItemsWD.SILVERAN.get());
                        pOutput.accept(ItemsWD.HANDFUL_YADOGA.get());
                        pOutput.accept(ItemsWD.A_DROP_OF_LOVE.get());
                        pOutput.accept(ItemsWD.CLEANSING_DECOCTION.get());
                        pOutput.accept(ItemsWD.HANDFUL_NETHER.get());
                        pOutput.accept(ItemsWD.SPARKLING_POLLEN.get());
                        pOutput.accept(ItemsWD.MUSIC_DISK_1.get());
                        pOutput.accept(ItemsWD.MUSIC_DISK_2.get());
                        pOutput.accept(ItemsWD.MUSIC_DISK_3.get());
                        pOutput.accept(ItemsWD.CRIMSON_BONE_MEAL.get());
                        pOutput.accept(ItemsWD.WARPED_BONE_MEAL.get());
                        pOutput.accept(ItemsWD.GRASS_BONE_MEAL.get());
                        pOutput.accept(ItemsWD.MYCELIUM_BONE_MEAL.get());
                        pOutput.accept(ItemsWD.COCKED_SLICING_GOATS_MEAT.get());
                        pOutput.accept(ItemsWD.RAW_SLICING_GOATS_MEAT.get());
                        pOutput.accept(ItemsWD.COASTAL_STEEP_FIBERS.get());
                        pOutput.accept(ItemsWD.COASTAL_STEEP_FLOWER.get());
                        pOutput.accept(ItemsWD.FLAME_ARROW.get());
                        pOutput.accept(ItemsWD.CHARMING_BERRIES.get());
                        pOutput.accept(ItemsWD.SOUL_STONE.get());
                        pOutput.accept(ItemsWD.ROSE_OF_THE_MURDERER.get());
                        pOutput.accept(ItemsWD.ROTTEN_PIE.get());
                        pOutput.accept(ItemsWD.WIZARD_PIE.get());
                        pOutput.accept(ItemsWD.WIZARD_PIE_SLICE.get());
                        pOutput.accept(ItemsWD.ROTTEN_PIE_SLICE.get());
                        pOutput.accept(ItemsWD.ROBIN_STICK.get());
                        pOutput.accept(ItemsWD.DRAGOLIT_INGOT.get());
                        pOutput.accept(ItemsWD.STRANGE_SCRAP.get());
                        pOutput.accept(ItemsWD.CLEAR_DRAGOLIT_NUGGET.get());
                        pOutput.accept(ItemsWD.DRAGOLIT_RAPIER.get());
                        pOutput.accept(ItemsWD.HEALING_DEW.get());
                        pOutput.accept(ItemsWD.TINCTURE_FOR_PIGLIN.get());
                        pOutput.accept(ItemsWD.SPATIAL_ORCHID.get());
                        pOutput.accept(ItemsWD.COASTAL_STEEP.get());
                        pOutput.accept(ItemsWD.GOLDEN_CHEST_KING_PILLAGER.get());
                        pOutput.accept(ItemsWD.THE_PILLAGERS_KEY.get());
                        pOutput.accept(ItemsWD.THE_KING_PILLAGERS_KEY.get());
                        pOutput.accept(ItemsWD.MAG_ELYTRA.get());
                        pOutput.accept(ItemsWD.DRAGOLITE_UPGRADE_SMITHING_TEMPLATE.get());
                        pOutput.accept(ItemsWD.MEADOW_GOLDEN_FLOWER.get());
                        pOutput.accept(ItemsWD.PICKLED_SHISH_KEBAB.get());
                        pOutput.accept(ItemsWD.APPLE_SAPLING.get());
                        pOutput.accept(ItemsWD.APPLE_LEAVES.get());
                        pOutput.accept(ItemsWD.APPLE_LEAVES_STAGES.get());
                        pOutput.accept(ItemsWD.APPLE_SIGN.get());
                        pOutput.accept(ItemsWD.APPLE_HANGING_SIGN.get());
                        pOutput.accept(ItemsWD.APPLE_BOAT.get());
                        pOutput.accept(ItemsWD.APPLE_CHEST_BOAT.get());
                        pOutput.accept(ItemsWD.WAFFLES.get());
                        pOutput.accept(ItemsWD.BERRIES_WAFFLES.get());
                        pOutput.accept(ItemsWD.APPLE_WAFFLES.get());
                        pOutput.accept(ItemsWD.ICE_WAFFLES.get());
                        pOutput.accept(ItemsWD.RAW_WAFFLES.get());
                        pOutput.accept(ItemsWD.POISON_WAFFLES.get());
                        pOutput.accept(ItemsWD.CHARMING_WAFFLES.get());
                        pOutput.accept(ItemsWD.GLOW_BERRIES_WAFFLES.get());

                        //Blocks
                        pOutput.accept(BlocksWD.FIRE_STEM.get());
                        pOutput.accept(BlocksWD.POISON_BERRY.get());
                        pOutput.accept(BlocksWD.FREEZE_BERRIES.get());
                        pOutput.accept(BlocksWD.WARPED_WART.get());
                        pOutput.accept(BlocksWD.SUGAR_SACK.get());
                        pOutput.accept(BlocksWD.THE_PILLAGERS_CHEST.get());
                        pOutput.accept(BlocksWD.GOLDEN_CHEST_KING_PILLAGER.get());
                        pOutput.accept(BlocksWD.A_BLOCK_OF_SPARKING_POLLEN.get());
                        pOutput.accept(BlocksWD.ROSE_OF_GHOSTY_TEARS.get());
                        pOutput.accept(BlocksWD.DRAGOLIT_GRID.get());
                        pOutput.accept(BlocksWD.DRAGOLIT_BLOCK.get());
                        pOutput.accept(ItemsWD.DRAGOLITE_CAGE.get());
                        pOutput.accept(BlocksWD.STRANGE_CHIP.get());
                        pOutput.accept(BlocksWD.APPLE_LOG.get());
                        pOutput.accept(BlocksWD.APPLE_WOOD.get());
                        pOutput.accept(BlocksWD.STRIPPED_APPLE_LOG.get());
                        pOutput.accept(BlocksWD.STRIPPED_APPLE_WOOD.get());
                        pOutput.accept(BlocksWD.APPLE_PLANKS.get());
                        pOutput.accept(BlocksWD.APPLE_STAIRS.get());
                        pOutput.accept(BlocksWD.APPLE_SLAB.get());
                        pOutput.accept(BlocksWD.APPLE_FENCE.get());
                        pOutput.accept(BlocksWD.APPLE_FENCE_GATE.get());
                        pOutput.accept(BlocksWD.APPLE_BUTTON.get());
                        pOutput.accept(BlocksWD.APPLE_PRESSURE_PLATE.get());
                        pOutput.accept(BlocksWD.APPLE_DOOR.get());
                        pOutput.accept(BlocksWD.APPLE_TRAPDOOR.get());

                    })
                    .build());

}
