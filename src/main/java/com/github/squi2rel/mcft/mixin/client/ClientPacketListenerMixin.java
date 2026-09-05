package com.github.squi2rel.mcft.mixin.client;

import com.github.squi2rel.mcft.MCFTClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(at = @At("HEAD"), method = "clearLevel")
    public void clearLevel(CallbackInfo ci) {
        MCFTClient.connected = false;
    }
}
