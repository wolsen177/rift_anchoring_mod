package com.xmtools.riftanchoring.network;

import com.xmtools.riftanchoring.RiftAnchoring;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AnchorActivePayload(BlockPos pos, boolean active) implements CustomPacketPayload {

    public static final Type<AnchorActivePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RiftAnchoring.MOD_ID, "anchor_active")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorActivePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AnchorActivePayload decode(RegistryFriendlyByteBuf buf) {
            return new AnchorActivePayload(buf.readBlockPos(), buf.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, AnchorActivePayload payload) {
            buf.writeBlockPos(payload.pos());
            buf.writeBoolean(payload.active());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(AnchorActivePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
        });
    }
}
