package com.github.squi2rel.mcft.mixin.client;

import com.github.squi2rel.mcft.FTCuboid;
import com.github.squi2rel.mcft.ext.PartDefinitionExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerModel.class)
public class PlayerModelMixin {
    @Inject(at = @At("RETURN"), method = "createMesh")
    private static void hook(CubeDeformation deformation, boolean slim, CallbackInfoReturnable<MeshDefinition> cir) {
        ((PartDefinitionExtension) cir.getReturnValue().getRoot()).MCFT$isPlayerModel(true);
    }

    /**
     * 26.1 defers the actual vertex writing: renderers only submit a {@code ModelSubmit} node and the model is
     * rendered later from {@code ModelFeatureRenderer}. {@code setupAnim} is the last per-player call before
     * {@code renderToBuffer}, so it is the point where the cuboid owner can safely be published.
     */
    @Inject(at = @At("HEAD"), method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V")
    private void mcft$captureOwner(AvatarRenderState state, CallbackInfo ci) {
        FTCuboid.player = mcft$resolveOwner(state.id);
    }

    @Unique
    private static UUID mcft$resolveOwner(int entityId) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        Entity entity = level.getEntity(entityId);
        return entity == null ? null : entity.getUUID();
    }
}
