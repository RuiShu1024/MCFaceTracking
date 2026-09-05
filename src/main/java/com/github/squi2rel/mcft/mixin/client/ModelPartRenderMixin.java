package com.github.squi2rel.mcft.mixin.client;

import com.github.squi2rel.mcft.FTCuboid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(value = ModelPart.class, priority = 900)
public class ModelPartRenderMixin {
    @Unique
    private static final Class<?> mcft$sodiumModelPartData = mcft$findSodiumModelPartData();

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mcft$render(
            PoseStack matrices, VertexConsumer vertices, int light, int overlay, int color, CallbackInfo ci
    ) {
        ModelPart part = (ModelPart) (Object) this;
        if (mcft$sodiumModelPartData == null || !mcft$sodiumModelPartData.isInstance(part)) return;
        ModelPartAccessor accessor = (ModelPartAccessor) (Object) part;
        List<ModelPart.Cube> cubes = accessor.getCubes();
        if (!mcft$containsCustomCuboid(cubes)) return;
        mcft$renderVanilla(part, accessor, cubes, matrices, vertices, light, overlay, color);
        ci.cancel();
    }

    @Unique
    private static Class<?> mcft$findSodiumModelPartData() {
        for (String name : new String[]{
                "net.caffeinemc.mods.sodium.client.render.immediate.model.ModelPartData",
                "me.jellysquid.mods.sodium.client.render.immediate.model.ModelPartData"
        }) {
            try {
                return Class.forName(name, false, ModelPart.class.getClassLoader());
            } catch (ClassNotFoundException | LinkageError ignored) {
            }
        }
        return null;
    }

    @Unique
    private static boolean mcft$containsCustomCuboid(List<ModelPart.Cube> cubes) {
        for (ModelPart.Cube cube : cubes) {
            if (cube instanceof FTCuboid) return true;
        }
        return false;
    }

    @Unique
    private static void mcft$renderVanilla(
            ModelPart part, ModelPartAccessor accessor, List<ModelPart.Cube> cubes,
            PoseStack matrices, VertexConsumer vertices, int light, int overlay, int color
    ) {
        if (!part.visible) return;
        Map<String, ModelPart> children = accessor.getChildren();
        if (cubes.isEmpty() && children.isEmpty()) return;

        matrices.pushPose();
        part.translateAndRotate(matrices);
        if (!part.skipDraw) {
            PoseStack.Pose entry = matrices.last();
            for (ModelPart.Cube cube : cubes) {
                cube.compile(entry, vertices, light, overlay, color);
            }
        }
        for (ModelPart child : children.values()) {
            child.render(matrices, vertices, light, overlay, color);
        }
        matrices.popPose();
    }
}
