package com.github.squi2rel.mcft;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class ServerPacketHandler {
    public static <P extends CustomPacketPayload> void registerC2S(CustomPacketPayload.Type<P> id, StreamCodec<FriendlyByteBuf, P> codec, BiConsumer<P, ServerPlayer> receiver) {
        PayloadTypeRegistry.serverboundPlay().register(id, codec);
        ServerPlayNetworking.registerGlobalReceiver(id, (packet, context) -> receiver.accept(packet, context.player()));
    }

    public static <P extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<P> id, StreamCodec<FriendlyByteBuf, P> codec) {
        PayloadTypeRegistry.clientboundPlay().register(id, codec);
    }

    public static <P extends CustomPacketPayload> void sendS2C(ServerPlayer player, P packet) {
        ServerPlayNetworking.send(player, packet);
    }
}
