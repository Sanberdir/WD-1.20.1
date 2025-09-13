package ru.imaginaerum.wd.common.blocks.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.blocks.BlocksWD;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, WD.MODID);

    public static final RegistryObject<BlockEntityType<GlowingJamBlockEntity>> GLOWING_JAM =
            BLOCK_ENTITIES.register("glowing_jam", () ->
                    BlockEntityType.Builder.of(GlowingJamBlockEntity::new,
                            BlocksWD.GLOWING_JAM.get()).build(null));

    public static final RegistryObject<BlockEntityType<RoseMurdererBlockEntity>> ROSE_MURDERER =
            BLOCK_ENTITIES.register("rose_murderer", () ->
                    BlockEntityType.Builder.of(RoseMurdererBlockEntity::new,
                            BlocksWD.ROSE_OF_THE_MURDERER.get()).build(null));

    public static final RegistryObject<BlockEntityType<DragoliteCageBlockEntity>> DRAGOLITE_CAGE_ENTITY =
            BLOCK_ENTITIES.register("dragolite_cage_entity", () ->
                    BlockEntityType.Builder.of(DragoliteCageBlockEntity::new,
                            BlocksWD.DRAGOLITE_CAGE.get()).build(null));

    public static final RegistryObject<BlockEntityType<EchotronBlockEntity>> ECHOTRON_ENTITY =
            BLOCK_ENTITIES.register("echotron_entity", () ->
                    BlockEntityType.Builder.of(EchotronBlockEntity::new,
                            BlocksWD.ECHOTRON.get()).build(null));

    public static final RegistryObject<BlockEntityType<ModSignBlockEntity>> APPLE_SIGN =
            BLOCK_ENTITIES.register("apple_sign", () ->
                    BlockEntityType.Builder.of(ModSignBlockEntity::new,
                            BlocksWD.APPLE_SIGN.get(), BlocksWD.APPLE_WALL_SIGN.get()).build(null));

    public static final RegistryObject<BlockEntityType<ModHangingSignBlockEntity>> APPLE_HANGING_SIGN =
            BLOCK_ENTITIES.register("apple_hanging_sign", () ->
                    BlockEntityType.Builder.of(ModHangingSignBlockEntity::new,
                            BlocksWD.APPLE_HANGING_SIGN.get(), BlocksWD.APPLE_WALL_HANGING_SIGN.get()).build(null));
}