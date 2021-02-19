package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EntityRenderer.class)
public abstract class Mixin_PreserveDepthDuringHandRendering {
    @ModifyArg(
            method = "updateCameraAndRender(FJ)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"),
            index = 0
    )
    private int replayModRender_skipClearWhenRecordingDepth(int mask) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) this).replayModRender_getHandler();
        if (handler != null && handler.getSettings().isDepthMap()) {
            mask = mask & ~GL11.GL_DEPTH_BUFFER_BIT;
        }
        return mask;
    }
}
