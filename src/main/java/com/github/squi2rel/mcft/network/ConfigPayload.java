package com.github.squi2rel.mcft.network;

import com.github.squi2rel.mcft.MCFT;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfigPayload(String version, int fps) implements CustomPacketPayload {
    public static final Identifier CONFIG_PAYLOAD_ID = Identifier.fromNamespaceAndPath(MCFT.MOD_ID, "config");
    public static final Type<ConfigPayload> ID = new Type<>(CONFIG_PAYLOAD_ID);
    public static final StreamCodec<FriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.ofMember((p, buf) -> {
        buf.writeUtf(p.version, 16);
        buf.writeInt(p.fps);
    }, buf -> new ConfigPayload(buf.readUtf(16), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
