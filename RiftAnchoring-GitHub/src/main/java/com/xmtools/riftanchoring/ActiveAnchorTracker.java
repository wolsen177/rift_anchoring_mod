package com.xmtools.riftanchoring;

import com.xmtools.riftanchoring.config.RiftAnchoringConfig;
import com.xmtools.riftanchoring.util.ActivationEffects;
import com.xmtools.riftanchoring.util.ObsidianBaseDetector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ActiveAnchorTracker {

    private static final Map<ResourceKey<Level>, Map<BlockPos, AnchorData>> ACTIVE_ANCHORS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, LodestoneData>> ACTIVE_LODESTONES = new ConcurrentHashMap<>();
    private static final List<DrainTask> DRAIN_TASKS = new LinkedList<>();

    private ActiveAnchorTracker() {}

    public record AnchorData(
            long lastConsumeGameTime,
            boolean infinite,
            long activationGameTime,
            boolean cloudStone
    ) {}

    public record LodestoneData(
            BlockPos anchorPos,
            long activationGameTime
    ) {}

    public static void activate(Level level, BlockPos pos, boolean infinite) {
        activate(level, pos, infinite, false);
    }

    public static void activate(Level level, BlockPos pos, boolean infinite, boolean cloudStone) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        ResourceKey<Level> dim = level.dimension();
        long gameTime = level.getGameTime();
        ACTIVE_ANCHORS.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
                .put(pos.immutable(), new AnchorData(gameTime, infinite, gameTime, cloudStone));

        ChunkPos chunkPos = new ChunkPos(pos);
        serverLevel.setChunkForced(chunkPos.x, chunkPos.z, true);

        RiftAnchoring.savePersistentState(serverLevel);
    }

    public static boolean isCloudStone(Level level, BlockPos pos) {
        Map<BlockPos, AnchorData> map = ACTIVE_ANCHORS.get(level.dimension());
        AnchorData data = map != null ? map.get(pos) : null;
        return data != null && data.cloudStone();
    }

    public static void deactivate(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        ResourceKey<Level> dim = level.dimension();
        Map<BlockPos, AnchorData> map = ACTIVE_ANCHORS.get(dim);
        boolean hadEntry = false;
        if (map != null) {
            hadEntry = map.containsKey(pos);
            map.remove(pos);
            if (map.isEmpty()) {
                ACTIVE_ANCHORS.remove(dim);
            }
        }

        deactivateAllLodestonesForAnchor(level, pos);

        ChunkPos chunkPos = new ChunkPos(pos);
        serverLevel.setChunkForced(chunkPos.x, chunkPos.z, false);

        if (hadEntry) {
            RiftAnchoring.savePersistentState(serverLevel);
        }
    }

    public static boolean isActive(Level level, BlockPos pos) {
        Map<BlockPos, AnchorData> map = ACTIVE_ANCHORS.get(level.dimension());
        return map != null && map.containsKey(pos);
    }

    public static boolean isInfinite(Level level, BlockPos pos) {
        Map<BlockPos, AnchorData> map = ACTIVE_ANCHORS.get(level.dimension());
        AnchorData data = map != null ? map.get(pos) : null;
        return data != null && data.infinite();
    }

    public static Map<BlockPos, AnchorData> getActiveAnchors(Level level) {
        return ACTIVE_ANCHORS.get(level.dimension());
    }

    public static void tickAnchors(ServerLevel level) {
        Map<BlockPos, AnchorData> anchors = ACTIVE_ANCHORS.get(level.dimension());
        if (anchors == null || anchors.isEmpty()) return;

        long gameTime = level.getGameTime();
        long intervalTicks = RiftAnchoringConfig.ENERGY_INTERVAL_SECONDS.get() * 20L;

        List<BlockPos> toRemove = new ArrayList<>();

        for (Map.Entry<BlockPos, AnchorData> entry : anchors.entrySet()) {
            BlockPos pos = entry.getKey();
            AnchorData data = entry.getValue();

            boolean baseValid = ObsidianBaseDetector.hasValidBase(level, pos);
            BlockState state = level.getBlockState(pos);
            boolean anchorValid = state.getBlock() instanceof RespawnAnchorBlock;

            if (!baseValid || !anchorValid) {
                toRemove.add(pos);
                if (anchorValid) {
                    triggerDestruction(level, pos);
                    activateDrain(level, pos);
                }
                continue;
            }

            if (data.infinite()) {
                spawnPersistentEffectsFor(level, pos, data);
                continue;
            }

            if (gameTime - data.lastConsumeGameTime() < intervalTicks) {
                spawnPersistentEffectsFor(level, pos, data);
                continue;
            }

            int charge = state.getValue(RespawnAnchorBlock.CHARGE);
            if (charge <= 0) {
                toRemove.add(pos);
                spawnDeactivateEffects(level, pos);
                continue;
            }

            int newCharge = charge - 1;
            level.setBlock(pos, state.setValue(RespawnAnchorBlock.CHARGE, newCharge), 3);
            anchors.put(pos, new AnchorData(gameTime, false, data.activationGameTime(), data.cloudStone()));

            if (newCharge <= 0) {
                toRemove.add(pos);
                spawnDeactivateEffects(level, pos);
            }
        }

        for (BlockPos pos : toRemove) {
            deactivate(level, pos);
        }
    }

    public static void tickLodestones(ServerLevel level) {
        Map<BlockPos, LodestoneData> lodestones = ACTIVE_LODESTONES.get(level.dimension());
        if (lodestones == null || lodestones.isEmpty()) return;

        List<BlockPos> toRemove = new ArrayList<>();

        for (Map.Entry<BlockPos, LodestoneData> entry : lodestones.entrySet()) {
            BlockPos pos = entry.getKey();
            LodestoneData data = entry.getValue();

            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.LODESTONE)) {
                toRemove.add(pos);
                continue;
            }

            boolean baseValid = ObsidianBaseDetector.hasValidBase(level, pos);
            if (!baseValid) {
                toRemove.add(pos);
                if (data.anchorPos() != null) {
                    setChunkForced(level, pos, false);
                }
                triggerLodestoneDestruction(level, pos);
                continue;
            }

            ActivationEffects.spawnLodestonePersistentEffects(level, pos);
        }

        for (BlockPos pos : toRemove) {
            deactivateLodestone(level, pos);
        }
    }

    public static void checkObsidianBreak(Level level, BlockPos brokenObsidianPos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos centerPos = brokenObsidianPos.above().offset(dx, 0, dz);
                if (isActive(level, centerPos)) {
                    BlockState state = level.getBlockState(centerPos);
                    if (state.getBlock() instanceof RespawnAnchorBlock) {
                        triggerDestruction(serverLevel, centerPos);
                        activateDrain(serverLevel, centerPos);
                    }
                    deactivate(level, centerPos);
                }
                if (isLodestoneActive(level, centerPos)) {
                    LodestoneData ld = getLodestoneData(level, centerPos);
                    if (ld != null && ld.anchorPos() != null) {
                        setChunkForced(level, centerPos, false);
                    }
                    triggerLodestoneDestruction(serverLevel, centerPos);
                    deactivateLodestone(level, centerPos);
                }
            }
        }
    }

    public static void checkLodestoneBreak(Level level, BlockPos brokenPos) {
        if (!(level instanceof ServerLevel)) return;

        if (isLodestoneActive(level, brokenPos)) {
            LodestoneData data = getLodestoneData(level, brokenPos);
            if (data != null && data.anchorPos() != null) {
                setChunkForced(level, brokenPos, false);
            }
            triggerLodestoneDestruction((ServerLevel) level, brokenPos);
            deactivateLodestone(level, brokenPos);
        }
    }

    public static void activateLodestoneStructure(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (isLodestoneActive(level, pos)) return;
        if (!ObsidianBaseDetector.hasValidBase(level, pos)) return;

        ResourceKey<Level> dim = level.dimension();
        long gameTime = level.getGameTime();
        ACTIVE_LODESTONES.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
                .put(pos.immutable(), new LodestoneData(null, gameTime));

        ActivationEffects.triggerLodestoneActivation(serverLevel, pos);
        level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 1.0F, 0.8F);

        RiftAnchoring.savePersistentState(serverLevel);
    }

    public static boolean tryBindLodestoneToAnchor(Level level, BlockPos lodestonePos, BlockPos anchorPos) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (!isActive(level, anchorPos)) return false;
        if (!isLodestoneActive(level, lodestonePos)) return false;

        ChunkPos anchorChunk = new ChunkPos(anchorPos);
        ChunkPos lodestoneChunk = new ChunkPos(lodestonePos);

        if (!isAdjacentChunk(anchorChunk, lodestoneChunk)) return false;

        int dx = lodestoneChunk.x - anchorChunk.x;
        int dz = lodestoneChunk.z - anchorChunk.z;
        if (Math.abs(dx) > 2 || Math.abs(dz) > 2) return false;

        int count = getLodestoneCountForAnchor(level, anchorPos);
        int max = RiftAnchoringConfig.MAX_LODESTONES_PER_ANCHOR.get();
        if (count >= max) return false;

        setChunkForced(level, lodestonePos, true);

        ResourceKey<Level> dim = level.dimension();
        ACTIVE_LODESTONES.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
                .put(lodestonePos.immutable(), new LodestoneData(anchorPos.immutable(), level.getGameTime()));

        if (RiftAnchoringConfig.ENABLE_LODESTONE_DEBUG_LOG.get()) {
            RiftAnchoring.LOGGER.info("[RiftAnchoring] Lodestone at {} bound to anchor at {} (chunk {} now forced)",
                    lodestonePos, anchorPos, lodestoneChunk);
        }

        RiftAnchoring.savePersistentState(serverLevel);

        return true;
    }

    public static boolean isAdjacentChunk(ChunkPos a, ChunkPos b) {
        int dx = Math.abs(a.x - b.x);
        int dz = Math.abs(a.z - b.z);
        return dx <= 1 && dz <= 1 && (dx != 0 || dz != 0);
    }

    public static int getLodestoneCountForAnchor(Level level, BlockPos anchorPos) {
        Map<BlockPos, LodestoneData> lodestones = ACTIVE_LODESTONES.get(level.dimension());
        if (lodestones == null) return 0;
        int count = 0;
        for (LodestoneData data : lodestones.values()) {
            if (data.anchorPos() != null && data.anchorPos().equals(anchorPos)) {
                count++;
            }
        }
        return count;
    }

    public static void deactivateAllLodestonesForAnchor(Level level, BlockPos anchorPos) {
        Map<BlockPos, LodestoneData> lodestones = ACTIVE_LODESTONES.get(level.dimension());
        if (lodestones == null) return;

        List<BlockPos> toUnbind = new ArrayList<>();
        for (Map.Entry<BlockPos, LodestoneData> entry : lodestones.entrySet()) {
            LodestoneData data = entry.getValue();
            if (data.anchorPos() != null && data.anchorPos().equals(anchorPos)) {
                toUnbind.add(entry.getKey());
            }
        }

        for (BlockPos pos : toUnbind) {
            setChunkForced(level, pos, false);
            lodestones.put(pos, new LodestoneData(null, level.getGameTime()));
        }

        if (!toUnbind.isEmpty() && level instanceof ServerLevel serverLevel) {
            RiftAnchoring.savePersistentState(serverLevel);
        }
    }

    public static boolean isLodestoneActive(Level level, BlockPos pos) {
        Map<BlockPos, LodestoneData> map = ACTIVE_LODESTONES.get(level.dimension());
        return map != null && map.containsKey(pos);
    }

    public static LodestoneData getLodestoneData(Level level, BlockPos pos) {
        Map<BlockPos, LodestoneData> map = ACTIVE_LODESTONES.get(level.dimension());
        return map != null ? map.get(pos) : null;
    }

    public static Map<BlockPos, LodestoneData> getActiveLodestones(Level level) {
        return ACTIVE_LODESTONES.get(level.dimension());
    }

    public static void deactivateLodestone(Level level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        Map<BlockPos, LodestoneData> map = ACTIVE_LODESTONES.get(dim);
        boolean hadEntry = false;
        if (map != null) {
            hadEntry = map.containsKey(pos);
            map.remove(pos);
            if (map.isEmpty()) {
                ACTIVE_LODESTONES.remove(dim);
            }
        }
        if (hadEntry && level instanceof ServerLevel serverLevel) {
            RiftAnchoring.savePersistentState(serverLevel);
        }
    }

    private static void setChunkForced(Level level, BlockPos pos, boolean forced) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ChunkPos chunkPos = new ChunkPos(pos);
        serverLevel.setChunkForced(chunkPos.x, chunkPos.z, forced);
    }

    private static void triggerLodestoneDestruction(ServerLevel level, BlockPos pos) {
        ActivationEffects.triggerLodestoneDestruction(level, pos);
    }

    private static void activateDrain(ServerLevel level, BlockPos anchorPos) {
        BlockState state = level.getBlockState(anchorPos);
        if (state.getBlock() instanceof RespawnAnchorBlock) {
            int charge = state.getValue(RespawnAnchorBlock.CHARGE);
            if (charge > 0) {
                synchronized (DRAIN_TASKS) {
                    DRAIN_TASKS.add(new DrainTask(level.dimension(), anchorPos.immutable(), charge));
                }
            }
        }
    }

    public static void tickDrain(ServerLevel level) {
        synchronized (DRAIN_TASKS) {
            if (DRAIN_TASKS.isEmpty()) return;
            long gameTime = level.getGameTime();
            Iterator<DrainTask> it = DRAIN_TASKS.iterator();
            while (it.hasNext()) {
                DrainTask task = it.next();
                if (!task.dimension.equals(level.dimension())) continue;

                BlockState state = level.getBlockState(task.pos);
                if (!(state.getBlock() instanceof RespawnAnchorBlock)) {
                    it.remove();
                    continue;
                }

                int charge = state.getValue(RespawnAnchorBlock.CHARGE);
                if (charge <= 0) {
                    triggerDestruction(level, task.pos);
                    it.remove();
                    continue;
                }

                int drainAmount = Math.min(1, charge);
                int newCharge = charge - drainAmount;
                level.setBlock(task.pos, state.setValue(RespawnAnchorBlock.CHARGE, newCharge), 3);

                double cx = task.pos.getX() + 0.5;
                double cy = task.pos.getY() + 0.5;
                double cz = task.pos.getZ() + 0.5;
                level.sendParticles(ParticleTypes.SMOKE, cx, cy, cz, 5, 0.3, 0.3, 0.3, 0.05);
                level.sendParticles(ParticleTypes.SOUL, cx, cy + 0.5, cz, 3, 0.2, 0.1, 0.2, 0.04);

                if (newCharge <= 0) {
                    triggerDestruction(level, task.pos);
                    it.remove();
                } else {
                    task.remainingCharge = newCharge;
                }
            }
        }
    }

    private static void triggerDestruction(ServerLevel level, BlockPos pos) {
        if (isCloudStone(level, pos)) {
            ActivationEffects.triggerAngelDestructionPenalty(level, pos);
        } else {
            ActivationEffects.triggerDestructionPenalty(level, pos);
        }
    }

    private static void spawnPersistentEffectsFor(ServerLevel level, BlockPos pos, AnchorData data) {
        if (data.cloudStone()) {
            ActivationEffects.spawnAngelPersistentEffects(level, pos);
        } else {
            ActivationEffects.spawnPersistentEffects(level, pos);
        }
    }

    public static long getRemainingTicks(Level level, BlockPos pos) {
        Map<BlockPos, AnchorData> map = ACTIVE_ANCHORS.get(level.dimension());
        AnchorData data = map != null ? map.get(pos) : null;
        if (data == null) return 0;
        long intervalTicks = RiftAnchoringConfig.ENERGY_INTERVAL_SECONDS.get() * 20L;
        long elapsed = level.getGameTime() - data.lastConsumeGameTime();
        return Math.max(0, intervalTicks - elapsed);
    }

    public static AnchorData getAnchorData(Level level, BlockPos pos) {
        Map<BlockPos, AnchorData> map = ACTIVE_ANCHORS.get(level.dimension());
        return map != null ? map.get(pos) : null;
    }

    public static CompoundTag saveAllState(ResourceKey<Level> dimension) {
        CompoundTag tag = new CompoundTag();

        ListTag anchorList = new ListTag();
        Map<BlockPos, AnchorData> dimAnchors = ACTIVE_ANCHORS.get(dimension);
        if (dimAnchors != null) {
            for (Map.Entry<BlockPos, AnchorData> entry : dimAnchors.entrySet()) {
                CompoundTag anchorTag = new CompoundTag();
                anchorTag.putInt("x", entry.getKey().getX());
                anchorTag.putInt("y", entry.getKey().getY());
                anchorTag.putInt("z", entry.getKey().getZ());
                AnchorData data = entry.getValue();
                anchorTag.putBoolean("infinite", data.infinite());
                anchorTag.putBoolean("cloudStone", data.cloudStone());
                anchorList.add(anchorTag);
            }
        }
        tag.put("anchors", anchorList);

        ListTag lodestoneList = new ListTag();
        Map<BlockPos, LodestoneData> dimLodestones = ACTIVE_LODESTONES.get(dimension);
        if (dimLodestones != null) {
            for (Map.Entry<BlockPos, LodestoneData> entry : dimLodestones.entrySet()) {
                CompoundTag lsTag = new CompoundTag();
                lsTag.putInt("x", entry.getKey().getX());
                lsTag.putInt("y", entry.getKey().getY());
                lsTag.putInt("z", entry.getKey().getZ());
                LodestoneData data = entry.getValue();
                if (data.anchorPos() != null) {
                    lsTag.putInt("ax", data.anchorPos().getX());
                    lsTag.putInt("ay", data.anchorPos().getY());
                    lsTag.putInt("az", data.anchorPos().getZ());
                }
                lodestoneList.add(lsTag);
            }
        }
        tag.put("lodestones", lodestoneList);

        return tag;
    }

    public static void restoreAllState(ServerLevel level, CompoundTag tag) {
        if (tag.contains("anchors", Tag.TAG_LIST)) {
            ListTag anchorList = tag.getList("anchors", Tag.TAG_COMPOUND);
            for (int i = 0; i < anchorList.size(); i++) {
                CompoundTag anchorTag = anchorList.getCompound(i);
                BlockPos pos = new BlockPos(
                        anchorTag.getInt("x"),
                        anchorTag.getInt("y"),
                        anchorTag.getInt("z"));
                boolean infinite = anchorTag.getBoolean("infinite");
                boolean cloudStone = anchorTag.getBoolean("cloudStone");

                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof RespawnAnchorBlock)) continue;
                if (!ObsidianBaseDetector.hasValidBase(level, pos)) continue;

                long gameTime = level.getGameTime();
                ACTIVE_ANCHORS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>())
                        .put(pos.immutable(), new AnchorData(gameTime, infinite, gameTime, cloudStone));

                ChunkPos chunkPos = new ChunkPos(pos);
                level.setChunkForced(chunkPos.x, chunkPos.z, true);

                if (RiftAnchoringConfig.ENABLE_LODESTONE_DEBUG_LOG.get()) {
                    RiftAnchoring.LOGGER.info("[RiftAnchoring] Restored anchor at {} (infinite={}, cloudStone={})",
                            pos, infinite, cloudStone);
                }
            }
        }

        if (tag.contains("lodestones", Tag.TAG_LIST)) {
            ListTag lodestoneList = tag.getList("lodestones", Tag.TAG_COMPOUND);
            for (int i = 0; i < lodestoneList.size(); i++) {
                CompoundTag lsTag = lodestoneList.getCompound(i);
                BlockPos pos = new BlockPos(
                        lsTag.getInt("x"),
                        lsTag.getInt("y"),
                        lsTag.getInt("z"));
                BlockPos anchorPos = null;
                if (lsTag.contains("ax")) {
                    anchorPos = new BlockPos(
                            lsTag.getInt("ax"),
                            lsTag.getInt("ay"),
                            lsTag.getInt("az"));
                }

                BlockState state = level.getBlockState(pos);
                if (!state.is(Blocks.LODESTONE)) continue;
                if (!ObsidianBaseDetector.hasValidBase(level, pos)) continue;

                ACTIVE_LODESTONES.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>())
                        .put(pos.immutable(), new LodestoneData(
                                anchorPos != null ? anchorPos.immutable() : null,
                                level.getGameTime()));

                if (anchorPos != null) {
                    ChunkPos chunkPos = new ChunkPos(pos);
                    level.setChunkForced(chunkPos.x, chunkPos.z, true);
                }

                if (RiftAnchoringConfig.ENABLE_LODESTONE_DEBUG_LOG.get()) {
                    RiftAnchoring.LOGGER.info("[RiftAnchoring] Restored lodestone at {} (bound to {})",
                            pos, anchorPos);
                }
            }
        }
    }

    private static void spawnDeactivateEffects(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.8F, 1.2F);
        level.sendParticles(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                10, 0.3, 0.5, 0.3, 0.02
        );
        ActivationEffects.triggerDeactivation(level, pos);
        RiftAnchoring.scheduleGlitch(level, pos, 8);
    }

    private static class DrainTask {
        final ResourceKey<Level> dimension;
        final BlockPos pos;
        int remainingCharge;

        DrainTask(ResourceKey<Level> dimension, BlockPos pos, int remainingCharge) {
            this.dimension = dimension;
            this.pos = pos;
            this.remainingCharge = remainingCharge;
        }
    }
}