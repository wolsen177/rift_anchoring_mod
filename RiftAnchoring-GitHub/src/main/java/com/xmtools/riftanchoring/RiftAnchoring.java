package com.xmtools.riftanchoring;

import com.mojang.logging.LogUtils;
import com.xmtools.riftanchoring.component.ModComponents;
import com.xmtools.riftanchoring.config.RiftAnchoringConfig;
import com.xmtools.riftanchoring.item.ModItems;
import com.xmtools.riftanchoring.network.AnchorActivePayload;
import com.xmtools.riftanchoring.util.ActivationEffects;
import com.xmtools.riftanchoring.util.ObsidianBaseDetector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(RiftAnchoring.MOD_ID)
public class RiftAnchoring {

    public static final String MOD_ID = "rift_anchoring";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final List<GlitchTask> GLITCH_TASKS = new LinkedList<>();
    private static final List<BindBeamTask> BIND_BEAM_TASKS = new LinkedList<>();

    private int energyTickCounter;

    public RiftAnchoring(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, RiftAnchoringConfig.SPEC, "rift-anchoring-common.toml");

        ModComponents.register(modEventBus);
        ModItems.register(modEventBus);

        modEventBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(this::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(this::onAnvilUpdate);
        NeoForge.EVENT_BUS.addListener(this::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onLevelLoad);

        LOGGER.info("Rift Anchoring - Chunk Loader Mod loaded!");
        LOGGER.info("  - 3x3 Obsidian base detection");
        LOGGER.info("  - Chunk loading on glowstone charge (1 hour per level)");
        LOGGER.info("  - Infinite glowstone crafting: see config (anvil/smithing/both)");
        LOGGER.info("  - Spawn point disabled in multiblock");
        LOGGER.info("  - Destruction penalty on base break");
        LOGGER.info("  - Persistent visual effects");
        LOGGER.info("  - Chunk Debug Wand");
        LOGGER.info("  - Rift Lodestone support");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                AnchorActivePayload.TYPE,
                AnchorActivePayload.STREAM_CODEC,
                AnchorActivePayload::handleClient
        );
    }

    private void onServerTick(ServerTickEvent.Post event) {
        energyTickCounter++;

        boolean energyTick = energyTickCounter % 20 == 0;
        boolean saveTick = energyTickCounter % 600 == 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (energyTick) {
                ActiveAnchorTracker.tickAnchors(level);
                ActiveAnchorTracker.tickLodestones(level);
            }
            if (saveTick) {
                savePersistentState(level);
            }
            ActiveAnchorTracker.tickDrain(level);
        }

        processGlitchTasks();
        processBindBeamTasks();
    }

    private void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        try {
            AnchorSavedData savedData = AnchorSavedData.get(serverLevel);
            if (savedData != null) {
                CompoundTag data = savedData.getSavedData();
                if (data != null && !data.isEmpty()) {
                    ActiveAnchorTracker.restoreAllState(serverLevel, data);
                    LOGGER.info("[RiftAnchoring] Restored state for dimension {}", serverLevel.dimension().location());
                }
            }
        } catch (Exception e) {
            LOGGER.error("[RiftAnchoring] Failed to restore state for dimension {}: {}",
                    serverLevel.dimension().location(), e.getMessage());
        }
    }

    public static void savePersistentState(ServerLevel level) {
        try {
            CompoundTag state = ActiveAnchorTracker.saveAllState(level.dimension());
            if (state == null) return;
            AnchorSavedData savedData = AnchorSavedData.get(level);
            if (savedData != null) {
                savedData.setData(state);
            }
        } catch (Exception e) {
            LOGGER.error("[RiftAnchoring] Failed to save state for dimension {}: {}",
                    level.dimension().location(), e.getMessage());
        }
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide) return;

        if (event.getState().is(Blocks.OBSIDIAN)) {
            ActiveAnchorTracker.checkObsidianBreak(level, event.getPos());
        } else if (event.getState().getBlock() instanceof net.minecraft.world.level.block.RespawnAnchorBlock) {
            ActiveAnchorTracker.checkObsidianBreak(level, event.getPos().below());
        } else if (event.getState().is(Blocks.LODESTONE)) {
            ActiveAnchorTracker.checkLodestoneBreak(level, event.getPos());
        }
    }

    private void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        if (event.getState().is(Blocks.LODESTONE) && ObsidianBaseDetector.hasValidBase(level, pos)
                && !ActiveAnchorTracker.isLodestoneActive(level, pos)) {
            ActivationEffects.triggerLodestoneStructureFormed(serverLevel, pos);
        }
        if (event.getState().is(Blocks.OBSIDIAN)) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).is(Blocks.LODESTONE) && ObsidianBaseDetector.hasValidBase(level, above)
                    && !ActiveAnchorTracker.isLodestoneActive(level, above)) {
                ActivationEffects.triggerLodestoneStructureFormed(serverLevel, above);
            }
        }
    }

    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.RECOVERY_COMPASS)) return;

        BlockHitResult hit = event.getHitVec();
        if (hit == null) return;

        BlockPos pos = hit.getBlockPos();
        if (!level.getBlockState(pos).is(Blocks.LODESTONE)) return;
        if (!ObsidianBaseDetector.hasValidBase(level, pos)) return;

        if (!ActiveAnchorTracker.isLodestoneActive(level, pos)) {
            ActiveAnchorTracker.activateLodestoneStructure(level, pos);
        }

        ItemStack compass = new ItemStack(ModItems.ANCHOR_GUIDE_COMPASS.get());
        compass.set(ModComponents.LODESTONE_BOUND.get(),
                level.dimension().location() + ";" + pos.getX() + ";" + pos.getY() + ";" + pos.getZ());

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (!player.getInventory().add(compass)) {
            player.drop(compass, false);
        }

        player.displayClientMessage(
                Component.translatable("message.rift_anchoring.lodestone_bound", pos.getX(), pos.getY(), pos.getZ()),
                true);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!"both".equals(RiftAnchoringConfig.INFINITE_GLOWSTONE_METHOD.get())
                && !"anvil".equals(RiftAnchoringConfig.INFINITE_GLOWSTONE_METHOD.get())) {
            return;
        }

        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (!left.is(Items.GLOWSTONE)) return;
        if (!right.is(Items.ENCHANTED_BOOK)) return;
        if (left.has(ModComponents.INFINITE_GLOWSTONE.get())) return;

        ItemStack output = left.copyWithCount(1);
        output.set(ModComponents.INFINITE_GLOWSTONE.get(), true);

        event.setOutput(output);
        event.setCost(5);
        event.setMaterialCost(1);
    }

    private void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.has(ModComponents.INFINITE_GLOWSTONE.get())) {
            String name = stack.getHoverName().getString();
            if ("云石".equals(name)) {
                event.getToolTip().add(Component.translatable("cloud_stone.tooltip"));
            } else {
                event.getToolTip().add(Component.translatable("infinite_glowstone.tooltip"));
            }
        }

        String bound = stack.get(ModComponents.LODESTONE_BOUND.get());
        if (bound != null && !bound.isEmpty()) {
            String[] parts = bound.split(";");
            if (parts.length == 4) {
                String dim = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                event.getToolTip().add(Component.translatable("item.rift_anchoring.anchor_guide_compass.bound",
                        dim, x, y, z));
            }
        } else if (stack.is(ModItems.ANCHOR_GUIDE_COMPASS.get())) {
            event.getToolTip().add(Component.translatable("item.rift_anchoring.anchor_guide_compass.unbound"));
        }
    }

    private void processGlitchTasks() {
        synchronized (GLITCH_TASKS) {
            Iterator<GlitchTask> it = GLITCH_TASKS.iterator();
            while (it.hasNext()) {
                GlitchTask task = it.next();
                ActivationEffects.spawnGlitchTick(task.level, task.anchorPos);
                task.remainingTicks--;
                if (task.remainingTicks <= 0) {
                    it.remove();
                }
            }
        }
    }

    public static void scheduleGlitch(ServerLevel level, BlockPos anchorPos, int ticks) {
        synchronized (GLITCH_TASKS) {
            GLITCH_TASKS.add(new GlitchTask(level, anchorPos, ticks));
        }
    }

    public static void scheduleBindBeam(ServerLevel level, BlockPos lodestonePos, BlockPos anchorPos, int ticks) {
        synchronized (BIND_BEAM_TASKS) {
            BIND_BEAM_TASKS.add(new BindBeamTask(level, lodestonePos, anchorPos, ticks));
        }
    }

    private void processBindBeamTasks() {
        synchronized (BIND_BEAM_TASKS) {
            Iterator<BindBeamTask> it = BIND_BEAM_TASKS.iterator();
            while (it.hasNext()) {
                BindBeamTask task = it.next();
                float progress = 1.0F - (float) task.remainingTicks / task.totalTicks;
                ActivationEffects.spawnLodestoneBindBeam(task.level, task.lodestonePos, task.anchorPos, progress);
                task.remainingTicks--;
                if (task.remainingTicks <= 0) {
                    it.remove();
                }
            }
        }
    }

    private static class GlitchTask {
        final ServerLevel level;
        final BlockPos anchorPos;
        int remainingTicks;

        GlitchTask(ServerLevel level, BlockPos anchorPos, int remainingTicks) {
            this.level = level;
            this.anchorPos = anchorPos;
            this.remainingTicks = remainingTicks;
        }
    }

    private static class BindBeamTask {
        final ServerLevel level;
        final BlockPos lodestonePos;
        final BlockPos anchorPos;
        final int totalTicks;
        int remainingTicks;

        BindBeamTask(ServerLevel level, BlockPos lodestonePos, BlockPos anchorPos, int remainingTicks) {
            this.level = level;
            this.lodestonePos = lodestonePos;
            this.anchorPos = anchorPos;
            this.totalTicks = remainingTicks;
            this.remainingTicks = remainingTicks;
        }
    }
}