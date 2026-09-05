package com.github.squi2rel.mcft.mixin.client;

import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(CubeDefinition.class)
public interface CubeDefinitionAccessor {
    @Accessor("origin")
    Vector3fc getOrigin();

    @Accessor("dimensions")
    Vector3fc getDimensions();

    @Accessor("grow")
    CubeDeformation getGrow();

    @Accessor("mirror")
    boolean isMirror();

    @Accessor("texCoord")
    UVPair getTexCoord();

    @Accessor("texScale")
    UVPair getTexScale();

    @Accessor("visibleFaces")
    Set<Direction> getVisibleFaces();
}
