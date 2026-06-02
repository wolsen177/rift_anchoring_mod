package com.xmtools.riftanchoring.component;

import com.mojang.serialization.Codec;
import com.xmtools.riftanchoring.RiftAnchoring;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RiftAnchoring.MOD_ID);

    public static final Supplier<DataComponentType<Boolean>> INFINITE_GLOWSTONE =
            COMPONENTS.register("infinite_glowstone", () ->
                    DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .build()
            );

    public static final Supplier<DataComponentType<String>> LODESTONE_BOUND =
            COMPONENTS.register("lodestone_bound", () ->
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build()
            );

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}