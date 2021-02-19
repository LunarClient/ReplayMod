package com.replaymod.render.blend.mixin;

import net.minecraft.client.particle.EntityFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityFX.class)
public interface ParticleAccessor
    //#if MC<10904
    extends EntityAccessor
    //#endif
{
    //#if MC>=10904
    //$$ @Accessor
    //$$ double getPrevPosX();
    //$$ @Accessor
    //$$ double getPrevPosY();
    //$$ @Accessor
    //$$ double getPrevPosZ();
    //$$ @Accessor
    //$$ double getPosX();
    //$$ @Accessor
    //$$ double getPosY();
    //$$ @Accessor
    //$$ double getPosZ();
    //#endif
}
