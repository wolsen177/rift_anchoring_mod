package com.xmtools.riftanchoring.item;

import com.xmtools.riftanchoring.RiftAnchoring;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(RiftAnchoring.MOD_ID);

    public static final DeferredItem<ChunkDebugItem> CHUNK_DEBUG_WAND =
            ITEMS.register("chunk_debug_wand", ChunkDebugItem::new);

    public static final DeferredItem<AnchorGuideCompass> ANCHOR_GUIDE_COMPASS =
            ITEMS.register("anchor_guide_compass", AnchorGuideCompass::new);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}