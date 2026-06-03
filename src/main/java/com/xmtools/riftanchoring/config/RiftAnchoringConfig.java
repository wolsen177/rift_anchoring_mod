package com.xmtools.riftanchoring.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class RiftAnchoringConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue ENERGY_INTERVAL_SECONDS;
    public static final ModConfigSpec.ConfigValue<String> INFINITE_GLOWSTONE_METHOD;
    public static final ModConfigSpec.ConfigValue<String> LODESTONE_RECIPE;
    public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_WAND;
    public static final ModConfigSpec.IntValue MAX_LODESTONES_PER_ANCHOR;
    public static final ModConfigSpec.BooleanValue ENABLE_LODESTONE_DEBUG_LOG;
    public static final ModConfigSpec.BooleanValue ENABLE_ECHO_SHARD_RECIPE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
            "Rift Anchoring Configuration",
            "Energy Interval Seconds: Time interval (in seconds) between automatic energy consumption.",
            "Each consumption reduces the respawn anchor's charge by 1 level.",
            "Default: 3600 seconds (1 hour). Range: 1 - 86400 (24 hours).",
            "",
            "Infinite Glowstone: Using infinite glowstone on a multiblock anchor",
            "makes chunk loading permanent (no energy drain).",
            "Two crafting methods available, configurable via infiniteGlowstoneMethod:",
            "  'anvil'    - Glowstone + Enchanted Book in anvil (costs 5 XP levels)",
            "  'smithing' - Glowstone + Nether Star + Netherite Upgrade Template in smithing table",
            "  'both'     - Both methods enabled (default)",
            "",
            "Lodestone Recipe: Controls which lodestone crafting recipe is available.",
            "Minecraft 1.21.5 changed lodestone from netherite to iron, this mod backports the iron recipe.",
            "  'iron'      - Only iron ingot recipe (8 iron + 1 chiseled stone bricks, matching 1.21.5+)",
            "  'netherite' - Only vanilla netherite recipe (8 chiseled stone bricks + 1 netherite ingot)",
            "  'both'      - Both iron and netherite recipes enabled (default)"
        );

        ENERGY_INTERVAL_SECONDS = builder
                .comment("Time interval (seconds) between automatic energy consumption steps (default: 3600 = 1 hour)")
                .defineInRange("energyIntervalSeconds", 3600, 1, 86400);

        INFINITE_GLOWSTONE_METHOD = builder
                .comment("Crafting method for infinite glowstone: 'anvil', 'smithing', or 'both' (default: both)")
                .define("infiniteGlowstoneMethod", "both",
                        s -> "anvil".equals(s) || "smithing".equals(s) || "both".equals(s));

        LODESTONE_RECIPE = builder
                .comment("Lodestone crafting recipe: 'iron' (8 iron + chiseled stone bricks), 'netherite' (vanilla), or 'both' (default: both)")
                .define("lodestoneRecipe", "both",
                        s -> "iron".equals(s) || "netherite".equals(s) || "both".equals(s));

        MAX_LODESTONES_PER_ANCHOR = builder
                .comment("Maximum number of rift lodestones per anchor within 5x5 chunk range (default: 25)")
                .defineInRange("maxLodestonesPerAnchor", 25, 1, 25);

        ENABLE_LODESTONE_DEBUG_LOG = builder
                .comment("Enable detailed debug log output for rift lodestone operations. Default: false")
                .define("enableLodestoneDebugLog", false);

        ENABLE_ECHO_SHARD_RECIPE = builder
                .comment("Enable crafting recipe for Echo Shards (4 Amethyst Shards + 1 Sculk = 1 Echo Shard). Default: true")
                .define("enableEchoShardRecipe", true);

        builder.push("debug");

        ENABLE_DEBUG_WAND = builder
                .comment("Enable the Chunk Debug Wand item. Default: true")
                .define("enableDebugWand", true);

        builder.pop();

        SPEC = builder.build();
    }
}