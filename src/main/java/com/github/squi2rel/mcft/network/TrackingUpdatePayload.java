package com.github.squi2rel.mcft.network;

import com.github.squi2rel.mcft.MCFT;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record TrackingUpdatePayload(UUID player, byte[] data) implements CustomPacketPayload {
    public static final Identifier TRACKING_UPDATE_PAYLOAD_ID = Identifier.fromNamespaceAndPath(MCFT.MOD_ID, "tracking_update");
    public static final Type<TrackingUpdatePayload> ID = new Type<>(TRACKING_UPDATE_PAYLOAD_ID);
    public static final StreamCodec<FriendlyByteBuf, TrackingUpdatePayload> CODEC = StreamCodec.ofMember((p, buf) -> {
        buf.writeUUID(p.player);
        buf.writeShort(p.data.length);
        buf.writeBytes(p.data);
    }, buf -> {
        UUID uuid = buf.readUUID();
        byte[] bytes = new byte[buf.readShort()];
        buf.readBytes(bytes);
        return new TrackingUpdatePayload(uuid, bytes);
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
