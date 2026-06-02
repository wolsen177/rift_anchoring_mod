package com.xmtools.riftanchoring.item;

import com.xmtools.riftanchoring.ActiveAnchorTracker;
import com.xmtools.riftanchoring.config.RiftAnchoringConfig;
import com.xmtools.riftanchoring.util.ObsidianBaseDetector;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ChunkDebugItem extends Item {

    private static final String KEY_BOUND = "RiftAnchoring:bound_anchor_pos";

    public ChunkDebugItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (!RiftAnchoringConfig.ENABLE_DEBUG_WAND.get()) {
            player.displayClientMessage(
                    Component.translatable("item.rift_anchoring.chunk_debug_wand.disabled"), true);
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (player.isShiftKeyDown()) {
            return handleBindMode(player, level, pos, stack);
        }

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof RespawnAnchorBlock && ActiveAnchorTracker.isActive(level, pos)) {
            showAnchorDetails(player, level, pos);
        } else if (state.is(Blocks.LODESTONE) && ActiveAnchorTracker.isLodestoneActive(level, pos)) {
            showLodestoneDetails(player, level, pos);
        } else {
            showChunkStatus(player, (ServerLevel) level, pos);
        }

        return InteractionResult.SUCCESS;
    }

    private InteractionResult handleBindMode(Player player, Level level, BlockPos pos, ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof RespawnAnchorBlock
                && ObsidianBaseDetector.hasValidBase(level, pos)
                && ActiveAnchorTracker.isActive(level, pos)) {

            ChunkPos chunkPos = new ChunkPos(pos);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    Component.translatable("item.rift_anchoring.chunk_debug_wand.bound",
                            chunkPos.x, chunkPos.z));

            player.displayClientMessage(
                    Component.translatable("item.rift_anchoring.chunk_debug_wand.bound_success",
                            chunkPos.x, chunkPos.z), false);
            return InteractionResult.SUCCESS;
        }

        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.bind_fail"), true);
        return InteractionResult.PASS;
    }

    private void showAnchorDetails(Player player, Level level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        boolean isMultiblock = ObsidianBaseDetector.hasValidBase(level, pos);
        boolean isActive = ActiveAnchorTracker.isActive(level, pos);
        boolean isInfinite = ActiveAnchorTracker.isInfinite(level, pos);

        BlockState state = level.getBlockState(pos);
        int charge = 0;
        if (state.getBlock() instanceof RespawnAnchorBlock) {
            charge = state.getValue(RespawnAnchorBlock.CHARGE);
        }

        long remainingTicks = ActiveAnchorTracker.getRemainingTicks(level, pos);
        long remainingSeconds = remainingTicks / 20;

        String infiniteText = isInfinite
                ? Component.translatable("item.rift_anchoring.chunk_debug_wand.status.yes").getString()
                : Component.translatable("item.rift_anchoring.chunk_debug_wand.status.no").getString();

        int lodestoneCount = ActiveAnchorTracker.getLodestoneCountForAnchor(level, pos);

        player.displayClientMessage(
                Component.literal("§6========== §eRespawn Anchor Info §6=========="), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.anchor_pos",
                        pos.getX(), pos.getY(), pos.getZ()), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.chunk_pos",
                        chunkPos.x, chunkPos.z), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.multiblock",
                        isMultiblock
                                ? Component.translatable("item.rift_anchoring.chunk_debug_wand.status.yes")
                                : Component.translatable("item.rift_anchoring.chunk_debug_wand.status.no")), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.is_active",
                        isActive
                                ? Component.translatable("item.rift_anchoring.chunk_debug_wand.status.yes")
                                : Component.translatable("item.rift_anchoring.chunk_debug_wand.status.no")), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.charge", charge), false);
        player.displayClientMessage(
                Component.literal("  §7Infinite: §f" + infiniteText), false);
        if (!isInfinite && isActive) {
            player.displayClientMessage(
                    Component.translatable("item.rift_anchoring.chunk_debug_wand.remaining_time",
                            remainingSeconds, remainingSeconds / 60, remainingSeconds / 3600), false);
        }
        player.displayClientMessage(
                Component.literal("  §7Lodestones: §f" + lodestoneCount + " / " + RiftAnchoringConfig.MAX_LODESTONES_PER_ANCHOR.get()), false);

        if (lodestoneCount > 0 && RiftAnchoringConfig.ENABLE_LODESTONE_DEBUG_LOG.get()) {
            logLodestoneDetails(level, pos);
        }
    }

    private void showLodestoneDetails(Player player, Level level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        boolean isActive = ActiveAnchorTracker.isLodestoneActive(level, pos);
        ActiveAnchorTracker.LodestoneData data = ActiveAnchorTracker.getLodestoneData(level, pos);
        boolean isBound = data != null && data.anchorPos() != null;
        boolean isChunkForced = false;
        if (level instanceof ServerLevel serverLevel) {
            isChunkForced = serverLevel.getForcedChunks().contains(chunkPos.toLong());
        }

        player.displayClientMessage(
                Component.literal("§6========== §eRift Lodestone Info §6=========="), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.lodestone_pos",
                        pos.getX(), pos.getY(), pos.getZ()), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.chunk_pos",
                        chunkPos.x, chunkPos.z), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.is_active",
                        isActive
                                ? Component.translatable("item.rift_anchoring.chunk_debug_wand.status.yes")
                                : Component.translatable("item.rift_anchoring.chunk_debug_wand.status.no")), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.is_forced",
                        isChunkForced
                                ? Component.translatable("item.rift_anchoring.chunk_debug_wand.status.yes")
                                : Component.translatable("item.rift_anchoring.chunk_debug_wand.status.no")), false);
        if (isBound) {
            BlockPos anchorPos = data.anchorPos();
            player.displayClientMessage(
                    Component.translatable("item.rift_anchoring.chunk_debug_wand.lodestone_bound_to",
                            anchorPos.getX(), anchorPos.getY(), anchorPos.getZ()), false);
        } else {
            player.displayClientMessage(
                    Component.translatable("item.rift_anchoring.chunk_debug_wand.lodestone_unbound"), false);
        }
    }

    private void logLodestoneDetails(Level level, BlockPos anchorPos) {
        com.xmtools.riftanchoring.RiftAnchoring.LOGGER.info("[RiftAnchoring] ===== Anchor Debug: {} =====", anchorPos);
        com.xmtools.riftanchoring.RiftAnchoring.LOGGER.info("[RiftAnchoring] Active forced chunks nearby:");

        ChunkPos anchorChunk = new ChunkPos(anchorPos);
        if (level instanceof ServerLevel serverLevel) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    ChunkPos cp = new ChunkPos(anchorChunk.x + dx, anchorChunk.z + dz);
                    if (serverLevel.getForcedChunks().contains(cp.toLong())) {
                        com.xmtools.riftanchoring.RiftAnchoring.LOGGER.info("[RiftAnchoring]   Chunk ({}, {}) is FORCED", cp.x, cp.z);
                    }
                }
            }
        }

        Map<BlockPos, ActiveAnchorTracker.LodestoneData> lodestones = ActiveAnchorTracker.getActiveLodestones(level);
        if (lodestones != null) {
            int bound = 0;
            int unbound = 0;
            int inRange = 0;
            for (Map.Entry<BlockPos, ActiveAnchorTracker.LodestoneData> entry : lodestones.entrySet()) {
                BlockPos lsPos = entry.getKey();
                ActiveAnchorTracker.LodestoneData ld = entry.getValue();
                ChunkPos lsChunk = new ChunkPos(lsPos);
                int dx = lsChunk.x - anchorChunk.x;
                int dz = lsChunk.z - anchorChunk.z;
                boolean in5x5 = Math.abs(dx) <= 2 && Math.abs(dz) <= 2;
                if (in5x5) inRange++;
                if (ld.anchorPos() != null && ld.anchorPos().equals(anchorPos)) {
                    bound++;
                    com.xmtools.riftanchoring.RiftAnchoring.LOGGER.info("[RiftAnchoring]   [BOUND] Lodestone at {} (chunk {}, {})", lsPos, lsChunk.x, lsChunk.z);
                } else if (ld.anchorPos() == null) {
                    unbound++;
                }
            }
            com.xmtools.riftanchoring.RiftAnchoring.LOGGER.info("[RiftAnchoring] 5x5 range: {} lodestones, Bound: {}, Unbound: {}", inRange, bound, unbound);
        }
    }

    private void showChunkStatus(Player player, ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;

        boolean isLoaded = level.isLoaded(pos);
        boolean isForceLoaded = level.getForcedChunks().contains(chunkPos.toLong());

        int ticketLevel;
        String ticketDesc;
        if (isForceLoaded) {
            ticketLevel = 31;
            ticketDesc = "FORCED";
        } else if (isLoaded) {
            ticketLevel = 33;
            ticketDesc = "TICKING";
        } else {
            ticketLevel = Integer.MAX_VALUE;
            ticketDesc = Component.translatable("item.rift_anchoring.chunk_debug_wand.unloaded").getString();
        }

        boolean hasNearbyForced = checkNearbyForced(level, chunkX, chunkZ);

        player.displayClientMessage(
                Component.literal("§6========== §eChunk Load Debug §6=========="), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.chunk_pos",
                        chunkX, chunkZ), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.block_pos",
                        pos.getX(), pos.getY(), pos.getZ()), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.is_loaded",
                        isLoaded
                                ? Component.translatable("item.rift_anchoring.chunk_debug_wand.status.yes")
                                : Component.translatable("item.rift_anchoring.chunk_debug_wand.status.no")), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.chunk_level",
                        ticketLevel, ticketDesc), false);
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.is_forced",
                        isForceLoaded
                                ? Component.translatable("item.rift_anchoring.chunk_debug_wand.status.yes")
                                : Component.translatable("item.rift_anchoring.chunk_debug_wand.status.no")), false);
        if (isForceLoaded) {
            player.displayClientMessage(
                    Component.translatable("item.rift_anchoring.chunk_debug_wand.ticket_type",
                            "RESPAWN_ANCHOR_LOADER"), false);
        }
        player.displayClientMessage(
                Component.translatable("item.rift_anchoring.chunk_debug_wand.nearby_forced",
                        hasNearbyForced
                                ? Component.translatable("item.rift_anchoring.chunk_debug_wand.status.yes")
                                : Component.translatable("item.rift_anchoring.chunk_debug_wand.status.no")), false);
    }

    private boolean checkNearbyForced(ServerLevel level, int centerX, int centerZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                ChunkPos cp = new ChunkPos(centerX + dx, centerZ + dz);
                if (level.getForcedChunks().contains(cp.toLong())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}