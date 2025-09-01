package ru.imaginaerum.wd.common.blocks;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PepperRegistry {
    public static final Set<BlockPos> PEPPER_BLOCKS = Collections.synchronizedSet(new HashSet<>());

    public static void registerPepper(BlockPos pos) {
        synchronized (PEPPER_BLOCKS) {
            PEPPER_BLOCKS.add(pos);
        }
    }

    public static void unregisterPepper(BlockPos pos) {
        synchronized (PEPPER_BLOCKS) {
            PEPPER_BLOCKS.remove(pos);
        }
    }

    public static Set<BlockPos> getPepperBlocksCopy() {
        synchronized (PEPPER_BLOCKS) {
            return new HashSet<>(PEPPER_BLOCKS);
        }
    }
}