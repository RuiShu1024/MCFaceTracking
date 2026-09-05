package com.github.squi2rel.mcft;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ClientPacketHandler {
    public static <P extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<P> id, StreamCodec<FriendlyByteBuf, P> codec, Consumer<P> receiver) {
        ClientPlayNetworking.registerGlobalReceiver(id, (packet, context) -> receiver.accept(packet));
    }

    public static <P extends CustomPacketPayload> void sendC2S(P packet) {
        ClientPlayNetworking.send(packet);
    }
}
