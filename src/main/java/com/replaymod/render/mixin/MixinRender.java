package com.replaymod.render.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.renderer.entity.Render;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public abstract class MixinRender {
    //#if MC>=11500
    //$$ @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    //#else
    //#if MC>=11400
    //$$ @Inject(method = "renderLivingLabel", at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "renderLivingLabel", at = @At("HEAD"), cancellable = true)
    //#endif
    //#endif
    private void replayModRender_areAllNamesHidden(CallbackInfo ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().entityRenderer).replayModRender_getHandler();
        if (handler != null && !handler.getSettings().isRenderNameTags()) {
            ci.cancel();
        }
    }
}
