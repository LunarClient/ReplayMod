package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

//#if MC>=11500
//$$ import net.minecraft.client.render.WorldRenderer;
//#else
import net.minecraft.client.renderer.EntityRenderer;
//#endif

/**
 * Forces the sky to always render when chroma keying is active. Ordinarily it only renders when the render distance is
 * at 4 or greater.
 */
//#if MC>=11500
//$$ @Mixin(WorldRenderer.class)
//#else
@Mixin(EntityRenderer.class)
//#endif
public abstract class Mixin_ChromaKeyForceSky {
    @Shadow @Final private Minecraft mc;

    //#if MC>=11500
    //$$ @ModifyConstant(method = "render", constant = @Constant(intValue = 4))
    //#else
    //#if MC>=11400
    //$$ @ModifyConstant(method = "updateCameraAndRender(FJ)V", constant = @Constant(intValue = 4))
    //#else
    @ModifyConstant(method = "updateCameraAndRender(FJ)V", constant = @Constant(intValue = 4))
    //#endif
    //#endif
    private int forceSkyWhenChromaKeying(int value) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) this.mc.entityRenderer).replayModRender_getHandler();
        if (handler != null) {
            ReadableColor color = handler.getSettings().getChromaKeyingColor();
            if (color != null) {
                return 0;
            }
        }
        return value;
    }
}
