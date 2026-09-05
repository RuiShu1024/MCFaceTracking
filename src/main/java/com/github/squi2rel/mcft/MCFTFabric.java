package com.github.squi2rel.mcft;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class MCFTFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        MCFT.onInitialize();

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            MCFTClient.onInitializeClient();
        }
    }
}
