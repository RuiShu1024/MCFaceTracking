package com.github.squi2rel.mcft;

import com.github.squi2rel.mcft.network.ConfigPayload;
import com.github.squi2rel.mcft.network.TrackingParamsPayload;
import com.github.squi2rel.mcft.network.TrackingUpdatePayload;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MCFT {
    public static final String MOD_ID = "mcft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ConcurrentHashMap<UUID, FTModel> models = new ConcurrentHashMap<>();

    public static String version = Platform.getVersion();
    public static Path configPath = Platform.getConfigPath();
    public static ServerConfig config;

    public static void onInitialize() {
        config = loadConfig(ServerConfig.class, configPath);

        ServerPacketHandler.registerC2S(TrackingParamsPayload.ID, TrackingParamsPayload.CODEC, (payload, p) -> {
            FTModel old = models.get(p.getUUID());
            if (old == null) LOGGER.info("Player {} is using MCFT", Objects.requireNonNull(p.getDisplayName()).getString());
            FTModel model = new FTModel(payload.eyeR(), payload.eyeL(), payload.mouth(), payload.flat());
            model.validate(true);
            if (old != null) model.enabled = old.enabled;
            models.put(p.getUUID(), model);
            if (model.enabled) {
                TrackingParamsPayload packet = new TrackingParamsPayload(p.getUUID(), model.eyeR, model.eyeL, model.mouth, model.isFlat);
                for (ServerPlayer player : p.level().getServer().getPlayerList().getPlayers()) ServerPacketHandler.sendS2C(player, packet);
            }
        });

        ServerPacketHandler.registerC2S(TrackingUpdatePayload.ID, TrackingUpdatePayload.CODEC, (payload, p) -> {
            FTModel model = models.get(p.getUUID());
            if (model == null || System.currentTimeMillis() - model.lastReceived + 10 < 1000 / config.fps) return;
            model.readSync(payload.data());
            model.validate(false);
            if (!model.enabled) {
                model.enabled = true;
                LOGGER.info("Player {} has OSC connected", Objects.requireNonNull(p.getDisplayName()).getString());
                TrackingParamsPayload packet = new TrackingParamsPayload(p.getUUID(), model.eyeR, model.eyeL, model.mouth, model.isFlat);
                for (ServerPlayer player : p.level().getServer().getPlayerList().getPlayers()) ServerPacketHandler.sendS2C(player, packet);
            }
            TrackingUpdatePayload packet = new TrackingUpdatePayload(p.getUUID(), payload.data());
            for (ServerPlayer player : p.level().getPlayers(player ->
                    player.position().closerThan(p.position(), config.syncRadius)
            )) ServerPacketHandler.sendS2C(player, packet);
        });

        ServerPacketHandler.registerS2C(TrackingParamsPayload.ID, TrackingParamsPayload.CODEC);
        ServerPacketHandler.registerS2C(TrackingUpdatePayload.ID, TrackingUpdatePayload.CODEC);
        ServerPacketHandler.registerS2C(ConfigPayload.ID, ConfigPayload.CODEC);

        Platform.register();
    }

    public static void onPlayerJoin(ServerPlayer player) {
        models.forEach((u, m) -> {
            if (m.enabled) ServerPacketHandler.sendS2C(player, new TrackingParamsPayload(u, m.eyeR, m.eyeL, m.mouth, m.isFlat));
        });
        ServerPacketHandler.sendS2C(player, new ConfigPayload(version, config.fps));
    }

    public static void onPlayerLeave(ServerPlayer player) {
        models.remove(player.getUUID());
    }

    public static <T> T loadConfig(Class<T> clazz, Path path) {
        try {
            return new Gson().fromJson(Files.readString(path), clazz);
        } catch (Exception e) {
            try {
                saveConfig(clazz.getDeclaredConstructor().newInstance(), path);
                return new Gson().fromJson(Files.readString(path), clazz);
            } catch (Exception ex) {
                RuntimeException th = new RuntimeException("Failed to load config file", ex);
                th.addSuppressed(e);
                throw th;
            }
        }
    }

    public static void saveConfig(Object config, Path path) {
        try {
            Files.writeString(path, new Gson().toJson(config));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
