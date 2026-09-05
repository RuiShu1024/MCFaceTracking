package com.github.squi2rel.mcft.network;

import com.github.squi2rel.mcft.MCFT;
import com.github.squi2rel.mcft.tracking.EyeTrackingRect;
import com.github.squi2rel.mcft.tracking.MouthTrackingRect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record TrackingParamsPayload(UUID player, EyeTrackingRect eyeR, EyeTrackingRect eyeL, MouthTrackingRect mouth, boolean flat) implements CustomPacketPayload {
    public static final Identifier TRACKING_PARAMS_PAYLOAD_ID = Identifier.fromNamespaceAndPath(MCFT.MOD_ID, "tracking_params");
    public static final Type<TrackingParamsPayload> ID = new Type<>(TRACKING_PARAMS_PAYLOAD_ID);
    public static final StreamCodec<FriendlyByteBuf, TrackingParamsPayload> CODEC = StreamCodec.ofMember((p, buf) -> {
        buf.writeUUID(p.player);
        p.eyeR.write(buf);
        p.eyeL.write(buf);
        p.mouth.write(buf);
        buf.writeBoolean(p.flat);
    }, buf -> new TrackingParamsPayload(buf.readUUID(), EyeTrackingRect.read(buf), EyeTrackingRect.read(buf), MouthTrackingRect.read(buf), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
