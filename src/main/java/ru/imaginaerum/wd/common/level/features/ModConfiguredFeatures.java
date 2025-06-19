package ru.imaginaerum.wd.common.level.features;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.PineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.RandomSpreadFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.BendingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import ru.imaginaerum.wd.WD;
import ru.imaginaerum.wd.common.blocks.BlocksWD;

import java.util.List;
import java.util.OptionalInt;

public class ModConfiguredFeatures {



    public static final ResourceKey<ConfiguredFeature<?, ?>> APPLE_KEY = registerKey("apple");

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        BeehiveDecorator beezApple012 = new BeehiveDecorator(0.12F);
        register(context, APPLE_KEY, Feature.TREE, createFancyOak().decorators(List.of(beezApple012)).build());
    }
    private static TreeConfiguration.TreeConfigurationBuilder createFancyOak() {
        return (new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(BlocksWD.APPLE_LOG.get()),
                new FancyTrunkPlacer(3, 11, 0),
                new WeightedStateProvider(  // Используем WeightedStateProvider для двух типов листвы
                        SimpleWeightedRandomList.<BlockState>builder()
                                .add(BlocksWD.APPLE_LEAVES.get().defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true), 3)  // Первый тип листвы с вероятностью 3
                                .add(BlocksWD.APPLE_LEAVES_STAGES.get().defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true), 1)
                                .build()
                ),
                new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(4), 4),
                new TwoLayersFeatureSize(0, 0, 0, OptionalInt.of(4))
        )).ignoreVines();
    }
    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(WD.MODID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
