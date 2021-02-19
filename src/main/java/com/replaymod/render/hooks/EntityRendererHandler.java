package com.replaymod.render.hooks;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.CaptureData;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.capturer.WorldRenderer;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.client.Minecraft;

//#if MC>=11500
//$$ import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC>=11400
//$$ import com.replaymod.core.events.PostRenderCallback;
//$$ import com.replaymod.core.events.PreRenderCallback;
//$$ import com.replaymod.core.events.PreRenderHandCallback;
//#else
//#if MC>=11400
//$$ import net.minecraftforge.fml.hooks.BasicEventHooks;
//#else
import net.minecraftforge.fml.common.FMLCommonHandler;
//#endif
//#endif

import java.io.IOException;

public class EntityRendererHandler extends EventRegistrations implements WorldRenderer {
    public final Minecraft mc = MCVer.getMinecraft();

    protected final RenderSettings settings;

    private final RenderInfo renderInfo;

    public CaptureData data;

    public boolean omnidirectional;

    public EntityRendererHandler(RenderSettings settings, RenderInfo renderInfo) {
        this.settings = settings;
        this.renderInfo = renderInfo;

        //#if MC>=11400
        //$$ on(PreRenderHandCallback.EVENT, () -> omnidirectional);
        //#endif

        ((IEntityRenderer) mc.entityRenderer).replayModRender_setHandler(this);
        register();
    }

    @Override
    public void renderWorld(final float partialTicks, CaptureData data) {
        this.data = data;
        renderWorld(partialTicks, 0);
    }

    public void renderWorld(float partialTicks, long finishTimeNano) {
        //#if MC>=11400
        //$$ PreRenderCallback.EVENT.invoker().preRender();
        //#else
        //#if MC>=11400
        //$$ BasicEventHooks.onRenderTickStart(partialTicks);
        //#else
        FMLCommonHandler.instance().onRenderTickStart(partialTicks);
        //#endif
        //#endif

        if (mc.theWorld != null && mc.thePlayer != null) {
            //#if MC>=11500
            //$$ mc.gameRenderer.renderWorld(partialTicks, finishTimeNano, new MatrixStack());
            //#else
            mc.entityRenderer.renderWorld(partialTicks, finishTimeNano);
            //#endif
        }

        //#if MC>=11400
        //$$ PostRenderCallback.EVENT.invoker().postRender();
        //#else
        //#if MC>=11400
        //$$ BasicEventHooks.onRenderTickEnd(partialTicks);
        //#else
        FMLCommonHandler.instance().onRenderTickEnd(partialTicks);
        //#endif
        //#endif
    }

    @Override
    public void close() throws IOException {
        ((IEntityRenderer) mc.entityRenderer).replayModRender_setHandler(null);
        unregister();
    }

    @Override
    public void setOmnidirectional(boolean omnidirectional) {
        this.omnidirectional = omnidirectional;
    }

    public RenderSettings getSettings() {
        return this.settings;
    }

    public RenderInfo getRenderInfo() {
        return this.renderInfo;
    }

    public interface IEntityRenderer {
        void replayModRender_setHandler(EntityRendererHandler handler);
        EntityRendererHandler replayModRender_getHandler();
    }
}
