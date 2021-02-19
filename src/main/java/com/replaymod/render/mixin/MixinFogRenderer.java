//#if MC>=11400
//$$ package com.replaymod.render.mixin;
//$$
//$$ import com.replaymod.core.versions.MCVer;
//$$ import com.replaymod.render.hooks.EntityRendererHandler;
//$$ import net.minecraft.client.renderer.FogRenderer;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//#if MC>=11400
//$$ import net.minecraft.client.renderer.ActiveRenderInfo;
//#endif
//$$
//$$ @Mixin(FogRenderer.class)
//$$ public abstract class MixinFogRenderer {
//$$     @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
//$$     private
    //#if MC>=11500
    //$$ static
    //#endif
//$$     void replayModRender_onSetupFog(CallbackInfo ci) {
//$$         EntityRendererHandler handler =
//$$                 ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
//$$         if (handler == null) return;
//$$         if (handler.getSettings().getChromaKeyingColor() != null) {
//$$             ci.cancel();
//$$         }
//$$     }
//$$ }
//#endif
