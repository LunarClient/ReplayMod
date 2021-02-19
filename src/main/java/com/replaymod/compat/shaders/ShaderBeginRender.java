//#if MC>=10800
package com.replaymod.compat.shaders;

import com.replaymod.render.hooks.EntityRendererHandler;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.client.Minecraft;

import java.lang.reflect.InvocationTargetException;

import static com.replaymod.core.versions.MCVer.getRenderPartialTicks;

//#if FABRIC>=1
//$$ import com.replaymod.core.events.PreRenderCallback;
//#else
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
//#endif

public class ShaderBeginRender extends EventRegistrations {

    private final Minecraft mc = Minecraft.getMinecraft();

    /**
     *  Invokes Shaders#beginRender when rendering a video,
     *  as this would usually get called by EntityRenderer#renderWorld,
     *  which we're not calling during rendering.
     */
    //#if FABRIC>=1
    //$$ { on(PreRenderCallback.EVENT, this::onRenderTickStart); }
    //$$ private void onRenderTickStart() {
    //#else
    @SubscribeEvent
    public void onRenderTickStart(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
    //#endif
        if (ShaderReflection.shaders_beginRender == null) return;
        if (ShaderReflection.config_isShaders == null) return;

        try {
            // check if video is being rendered
            if (((EntityRendererHandler.IEntityRenderer) mc.entityRenderer).replayModRender_getHandler() == null)
                return;

            // check if Shaders are enabled
            if (!(boolean) (ShaderReflection.config_isShaders.invoke(null))) return;

            ShaderReflection.shaders_beginRender.invoke(null, mc,
                    //#if MC>=11400
                    //$$ mc.gameRenderer.getActiveRenderInfo(),
                    //#endif
                    getRenderPartialTicks(), 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
//#endif
