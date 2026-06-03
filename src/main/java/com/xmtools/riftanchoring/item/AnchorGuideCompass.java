package com.xmtools.riftanchoring.item;

import com.xmtools.riftanchoring.ActiveAnchorTracker;
import com.xmtools.riftanchoring.RiftAnchoring;
import com.xmtools.riftanchoring.component.ModComponents;
import com.xmtools.riftanchoring.config.RiftAnchoringConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;

public class AnchorGuideCompass extends Item {

    public AnchorGuideCompass() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos targetPos = context.getClickedPos();
        if (!(level.getBlockState(targetPos).getBlock() instanceof RespawnAnchorBlock)) {
            return InteractionResult.PASS;
        }
        if (!ActiveAnchorTracker.isActive(level, targetPos)) {
            player.displayClientMessage(
                    Component.translatable("message.rift_anchoring.compass_anchor_inactive"), true);
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        String bound = stack.get(ModComponents.LODESTONE_BOUND.get());
        if (bound == null || bound.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.rift_anchoring.compass_not_bound"), true);
            return InteractionResult.PASS;
        }

        String[] parts = bound.split(";");
        if (parts.length != 4) return InteractionResult.PASS;

        BlockPos lodestonePos = new BlockPos(
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]));

        if (!ActiveAnchorTracker.isLodestoneActive(level, lodestonePos)) {
            player.displayClientMessage(
                    Component.translatable("message.rift_anchoring.lodestone_not_active"), true);
            return InteractionResult.PASS;
        }

        ActiveAnchorTracker.LodestoneData ld = ActiveAnchorTracker.getLodestoneData(level, lodestonePos);
        if (ld != null && ld.anchorPos() != null) {
            if (RiftAnchoringConfig.ENABLE_COMPASS_BIDIRECTIONAL_CONVERSION.get()
                    && ld.anchorPos().equals(targetPos)) {
                ActiveAnchorTracker.unbindLodestone(level, lodestonePos);

                ItemStack recoveryCompass = new ItemStack(Items.RECOVERY_COMPASS);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                if (!player.getInventory().add(recoveryCompass)) {
                    player.drop(recoveryCompass, false);
                }

                player.displayClientMessage(
                        Component.translatable("message.rift_anchoring.compass_reverted"), true);

                if (level instanceof ServerLevel serverLevel) {
                    level.playSound(null, targetPos, net.minecraft.sounds.SoundEvents.LODESTONE_COMPASS_LOCK,
                            net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.5F);
                }

                return InteractionResult.SUCCESS;
            }
            player.displayClientMessage(
                    Component.translatable("message.rift_anchoring.lodestone_already_bound"), true);
            return InteractionResult.PASS;
        }

        boolean success = ActiveAnchorTracker.tryBindLodestoneToAnchor(level, lodestonePos, targetPos);
        if (success) {
            ChunkPos lodestoneChunk = new ChunkPos(lodestonePos);
            player.displayClientMessage(
                    Component.translatable("message.rift_anchoring.compass_bind_success",
                            lodestoneChunk.x, lodestoneChunk.z), false);

            if (level instanceof ServerLevel serverLevel) {
                RiftAnchoring.scheduleBindBeam(serverLevel, lodestonePos, targetPos, 20);
            }

            if (!player.getAbilities().instabuild) {
                if (RiftAnchoringConfig.ENABLE_COMPASS_BIDIRECTIONAL_CONVERSION.get()) {
                    ItemStack recoveryCompass = new ItemStack(Items.RECOVERY_COMPASS);
                    stack.shrink(1);
                    if (!player.getInventory().add(recoveryCompass)) {
                        player.drop(recoveryCompass, false);
                    }
                    player.displayClientMessage(
                            Component.translatable("message.rift_anchoring.compass_reverted"), true);
                } else {
                    stack.shrink(1);
                }
            }
        } else {
            ChunkPos anchorChunk = new ChunkPos(targetPos);
            ChunkPos lodestoneChunk = new ChunkPos(lodestonePos);
            int dx = lodestoneChunk.x - anchorChunk.x;
            int dz = lodestoneChunk.z - anchorChunk.z;

            if (Math.abs(dx) > 2 || Math.abs(dz) > 2) {
                player.displayClientMessage(
                        Component.translatable("message.rift_anchoring.compass_out_of_range"), true);
            } else if (!ActiveAnchorTracker.isAdjacentChunk(anchorChunk, lodestoneChunk)) {
                player.displayClientMessage(
                        Component.translatable("message.rift_anchoring.compass_not_adjacent"), true);
            } else {
                int count = ActiveAnchorTracker.getLodestoneCountForAnchor(level, targetPos);
                player.displayClientMessage(
                        Component.translatable("message.rift_anchoring.compass_limit_reached", count), true);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        String bound = stack.get(ModComponents.LODESTONE_BOUND.get());
        if (bound != null && !bound.isEmpty()) {
            String[] parts = bound.split(";");
            if (parts.length == 4) {
                return Component.translatable("item.rift_anchoring.anchor_guide_compass",
                        parts[1], parts[2], parts[3]);
            }
        }
        return Component.translatable("item.rift_anchoring.anchor_guide_compass.unbound");
    }
}