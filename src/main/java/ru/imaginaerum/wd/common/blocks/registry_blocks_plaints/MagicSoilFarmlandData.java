package ru.imaginaerum.wd.common.blocks.registry_blocks_plaints;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public class MagicSoilFarmlandData extends SavedData {
    private final Set<BlockPos> farmlands = new HashSet<>();

    public static MagicSoilFarmlandData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                tag -> fromNbt(tag),
                MagicSoilFarmlandData::new,
                "magic_farmland"
        );
    }

    public void add(BlockPos pos) {
        farmlands.add(pos.immutable());
        setDirty();
    }

    public void remove(BlockPos pos) {
        farmlands.remove(pos);
        setDirty();
    }

    public Set<BlockPos> getFarmlands() {
        return new HashSet<>(farmlands);
    }

    // сериализация
    public static MagicSoilFarmlandData fromNbt(CompoundTag tag) {
        MagicSoilFarmlandData data = new MagicSoilFarmlandData();
        ListTag list = tag.getList("farmlands", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag posTag = (CompoundTag) t;
            data.farmlands.add(new BlockPos(
                    posTag.getInt("x"),
                    posTag.getInt("y"),
                    posTag.getInt("z")
            ));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos pos : farmlands) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            list.add(posTag);
        }
        tag.put("farmlands", list);
        return tag;
    }
}

