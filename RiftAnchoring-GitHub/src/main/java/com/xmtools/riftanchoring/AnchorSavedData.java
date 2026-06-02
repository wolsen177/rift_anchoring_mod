package com.xmtools.riftanchoring;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class AnchorSavedData extends SavedData {

    private static final String DATA_NAME = "rift_anchoring";

    private CompoundTag savedData = new CompoundTag();

    public AnchorSavedData() {}

    public static SavedData.Factory<AnchorSavedData> factory() {
        return new SavedData.Factory<>(AnchorSavedData::new, AnchorSavedData::load);
    }

    public static AnchorSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    public void setData(CompoundTag data) {
        this.savedData = data;
        setDirty();
    }

    public CompoundTag getSavedData() {
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("anchors", savedData.getList("anchors", Tag.TAG_COMPOUND));
        tag.put("lodestones", savedData.getList("lodestones", Tag.TAG_COMPOUND));
        return tag;
    }

    private static AnchorSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AnchorSavedData data = new AnchorSavedData();
        CompoundTag saved = new CompoundTag();
        if (tag.contains("anchors", Tag.TAG_LIST)) {
            saved.put("anchors", tag.getList("anchors", Tag.TAG_COMPOUND));
        }
        if (tag.contains("lodestones", Tag.TAG_LIST)) {
            saved.put("lodestones", tag.getList("lodestones", Tag.TAG_COMPOUND));
        }
        data.savedData = saved;
        return data;
    }
}