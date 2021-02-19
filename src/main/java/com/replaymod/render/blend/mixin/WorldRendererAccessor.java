// FIXME 1.15
//#if MC>=10800 && MC<11500
package com.replaymod.render.blend.mixin;

import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(RenderGlobal.class)
public interface WorldRendererAccessor {
    @Accessor
    List getRenderInfos();
}
//#endif
