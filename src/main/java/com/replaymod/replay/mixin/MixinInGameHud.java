//#if MC>=11400
//$$ package com.replaymod.replay.mixin;
//$$
//$$ import com.replaymod.replay.events.RenderSpectatorCrosshairCallback;
//$$ import net.minecraft.client.gui.IngameGui;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//$$ @Mixin(IngameGui.class)
//$$ public class MixinInGameHud {
//$$     @Inject(method = "func_212913_a", at = @At("HEAD"), cancellable = true)
//$$     private void shouldRenderSpectatorCrosshair(CallbackInfoReturnable<Boolean> ci) {
//$$         Boolean state = RenderSpectatorCrosshairCallback.EVENT.invoker().shouldRenderSpectatorCrosshair();
//$$         if (state != null) {
//$$             ci.setReturnValue(state);
//$$         }
//$$     }
//$$ }
//#endif
