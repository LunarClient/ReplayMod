package com.replaymod.recording.mixin;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

//#if MC>=10904
//$$ import net.minecraft.network.datasync.DataParameter;
//#endif

@Mixin(EntityLivingBase.class)
public interface EntityLivingBaseAccessor {
    //#if MC>=10904
    //$$ @Accessor("HAND_STATES")
    //$$ @Nonnull
    //$$ @SuppressWarnings("ConstantConditions")
    //$$ static DataParameter<Byte> getLivingFlags() { return null; }
    //#endif
}
