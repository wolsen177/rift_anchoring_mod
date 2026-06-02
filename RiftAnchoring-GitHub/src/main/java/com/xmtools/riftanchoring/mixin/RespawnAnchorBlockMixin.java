package com.xmtools.riftanchoring.mixin;

import com.xmtools.riftanchoring.ActiveAnchorTracker;
import com.xmtools.riftanchoring.RiftAnchoring;
import com.xmtools.riftanchoring.component.ModComponents;
import com.xmtools.riftanchoring.util.ActivationEffects;
import com.xmtools.riftanchoring.util.ObsidianBaseDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RespawnAnchorBlock.class)
public class RespawnAnchorBlockMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void onCharge(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (level.isClientSide) return;
        if (!stack.is(Items.GLOWSTONE)) return;
        if (!ObsidianBaseDetector.hasValidBase(level, pos)) return;

        boolean infinite = stack.has(ModComponents.INFINITE_GLOWSTONE.get());
        boolean cloudStone = infinite && "云石".equals(stack.getHoverName().getString());

        if (!ActiveAnchorTracker.isActive(level, pos)) {
            ActiveAnchorTracker.activate(level, pos, infinite, cloudStone);
            level.playSound(null, pos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (cloudStone) {
                ActivationEffects.triggerAngelActivation((ServerLevel) level, pos);
            } else {
                ActivationEffects.triggerActivation((ServerLevel) level, pos);
            }
            RiftAnchoring.scheduleGlitch((ServerLevel) level, pos, 10);
        } else if (infinite && !ActiveAnchorTracker.isInfinite(level, pos)) {
            ActiveAnchorTracker.activate(level, pos, true);
        }
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void onChargeReturn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (level.isClientSide) return;
        if (!stack.is(Items.GLOWSTONE)) return;
        if (!ObsidianBaseDetector.hasValidBase(level, pos)) return;
        if (!ActiveAnchorTracker.isInfinite(level, pos)) return;

        BlockState currentState = level.getBlockState(pos);
        if (currentState.getBlock() instanceof RespawnAnchorBlock) {
            level.setBlock(pos, currentState.setValue(RespawnAnchorBlock.CHARGE, 4), 3);
        }
    }

    @Inject(
            method = "useWithoutItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onUseWithoutItem(
            BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) return;
        boolean isMultiblock = ObsidianBaseDetector.hasValidBase(level, pos);
        if (!isMultiblock) return;
        if (state.getValue(RespawnAnchorBlock.CHARGE) == 0) return;

        player.displayClientMessage(Component.translatable("message.rift_anchoring.cannot_set_spawn"), true);
        cir.setReturnValue(InteractionResult.PASS);
    }
}
