package com.github.squi2rel.mcft.mixin.client;

import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PartDefinition.class)
public interface PartDefinitionAccessor {
    @Accessor("cubes")
    List<CubeDefinition> getCubes();
}
