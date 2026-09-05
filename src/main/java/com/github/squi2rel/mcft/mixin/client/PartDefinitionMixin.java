package com.github.squi2rel.mcft.mixin.client;

import com.github.squi2rel.mcft.FTCuboid;
import com.github.squi2rel.mcft.MCFT;
import com.github.squi2rel.mcft.ext.PartDefinitionExtension;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PartDefinition.class)
public class PartDefinitionMixin implements PartDefinitionExtension {
    @Unique
    private boolean mcft$isPlayerModel = false;

    @Override
    public void MCFT$isPlayerModel(boolean is) {
        mcft$isPlayerModel = is;
    }

    @Inject(at = @At("RETURN"), method = "bake")
    public void bake(int textureWidth, int textureHeight, CallbackInfoReturnable<ModelPart> cir) {
        if (!mcft$isPlayerModel) return;
        ModelPart root = cir.getReturnValue();
        if (!root.hasChild(PartNames.HEAD)) return;
        ModelPart head = root.getChild(PartNames.HEAD);
        if (head.hasChild(PartNames.LEFT_EAR)) return; // piglin, or the deadmau5 ears layer
        ModelPartAccessor accessor = (ModelPartAccessor) (Object) head;
        List<ModelPart.Cube> cubes = accessor.getCubes();
        // PlayerCapeModel/PlayerEarsModel derive their layers from PlayerModel.createMesh and then call
        // clearRecursively(), so a head without geometry is a stripped copy rather than the body model.
        if (cubes.isEmpty()) return;
        PartDefinition headDefinition = ((PartDefinition) (Object) this).getChild(PartNames.HEAD);
        if (cubes.size() != 1 || headDefinition == null) {
            MCFT.LOGGER.warn("Player model is missing HEAD!");
            return;
        }
        List<CubeDefinition> definitions = ((PartDefinitionAccessor) (Object) headDefinition).getCubes();
        if (definitions.size() != 1) {
            MCFT.LOGGER.warn("Player model is missing HEAD!");
            return;
        }
        accessor.setCubes(List.of(FTCuboid.newInstance(definitions.get(0), textureWidth, textureHeight)));
    }
}
