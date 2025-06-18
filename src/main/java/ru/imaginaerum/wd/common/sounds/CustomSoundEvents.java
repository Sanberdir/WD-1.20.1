package ru.imaginaerum.wd.common.sounds;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.wd.WD;

public class CustomSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, WD.MODID);

    public static final RegistryObject<SoundEvent> NYAMNYAM =
            registerSoundEvents("nyamnyam");
    public static final RegistryObject<SoundEvent> GOLDEN_FLOWER =
            registerSoundEvents("golden_flower");
    public static final RegistryObject<SoundEvent> NYAMNYAM_END =
            registerSoundEvents("nyamnyam_end");
    public static final RegistryObject<SoundEvent> FLIES =
            registerSoundEvents("flies");
    public static final RegistryObject<SoundEvent> ROBIN_STICK =
            registerSoundEvents("robin_stick");
    public static final RegistryObject<SoundEvent> REZOLV_12_THE_LONG_WAY_HOME =
            registerSoundEvents("rezolv_12_the_long_way_home");
    public static final RegistryObject<SoundEvent> REZOLV_15_SO_LITTLE_BUT_SO_BIG =
            registerSoundEvents("rezolv_15_so_little_but_so_big");
    public static final RegistryObject<SoundEvent> REZOLV_4_WANDERING_MINSTREL =
            registerSoundEvents("rezolv_4_wandering_minstrel");



    private static RegistryObject<SoundEvent> registerSoundEvents(String name) {
        ResourceLocation id = new ResourceLocation(WD.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

}
