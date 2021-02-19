//#if MC>=11400
//$$ package com.replaymod.core.mixin;
//$$
//$$ import com.replaymod.core.events.KeyBindingEventCallback;
//$$ import com.replaymod.core.events.KeyEventCallback;
//$$ import net.minecraft.client.KeyboardListener;
//$$ import net.minecraft.client.MouseHelper;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ @Mixin(MouseHelper.class)
//$$ public class MixinMouse {
//$$     @Inject(method = "mouseButtonCallback", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;onTick(Lnet/minecraft/client/util/InputMappings$Input;)V", shift = At.Shift.AFTER))
//$$     private void afterKeyBindingTick(CallbackInfo ci) {
//$$         KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
//$$     }
//$$ }
//#endif
