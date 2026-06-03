package com.xmtools.riftanchoring.config;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ConfigCondition implements ICondition {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("rift_anchoring", "config_enabled");
    public static final ConfigCondition INSTANCE = new ConfigCondition();
    public static final MapCodec<ConfigCondition> CODEC = MapCodec.unit(INSTANCE);

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, "rift_anchoring");

    public static final Supplier<MapCodec<? extends ICondition>> CONFIG_ENABLED =
            CONDITION_SERIALIZERS.register("config_enabled", () -> CODEC);

    @Override
    public boolean test(IContext context) {
        return RiftAnchoringConfig.ENABLE_ECHO_SHARD_RECIPE.get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}