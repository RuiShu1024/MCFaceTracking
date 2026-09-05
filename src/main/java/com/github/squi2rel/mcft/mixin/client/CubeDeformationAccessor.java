package com.github.squi2rel.mcft.mixin.client;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CubeDeformation.class)
public interface CubeDeformationAccessor {
    @Accessor("growX")
    float getGrowX();

    @Accessor("growY")
    float getGrowY();

    @Accessor("growZ")
    float getGrowZ();
}
