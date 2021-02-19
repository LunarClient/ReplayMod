package com.replaymod.recording.gui;

import com.replaymod.core.SettingsRegistry;
import com.replaymod.core.versions.MCVer;
import com.replaymod.recording.Setting;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.MinecraftGuiRenderer;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import de.johni0702.minecraft.gui.versions.MatrixStack;

//#if FABRIC>=1
//$$ import de.johni0702.minecraft.gui.versions.callbacks.RenderHudCallback;
//#else
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#endif

import static com.replaymod.core.ReplayMod.TEXTURE;
import static com.replaymod.core.ReplayMod.TEXTURE_SIZE;
import static com.replaymod.core.versions.MCVer.*;
import static net.minecraft.client.renderer.GlStateManager.*;

/**
 * Renders overlay during recording.
 */
public class GuiRecordingOverlay extends EventRegistrations {
    private final Minecraft mc;
    private final SettingsRegistry settingsRegistry;
    private final GuiRecordingControls guiControls;

    public GuiRecordingOverlay(Minecraft mc, SettingsRegistry settingsRegistry, GuiRecordingControls guiControls) {
        this.mc = mc;
        this.settingsRegistry = settingsRegistry;
        this.guiControls = guiControls;
    }

    /**
     * Render the recording icon and text in the top left corner of the screen.
     */
    //#if FABRIC>=1
    //$$ { on(RenderHudCallback.EVENT, (stack, partialTicks) -> renderRecordingIndicator(stack)); }
    //$$ private void renderRecordingIndicator(MatrixStack stack) {
    //#else
    @SubscribeEvent
    public void renderRecordingIndicator(RenderGameOverlayEvent.Post event) {
        if (getType(event) != RenderGameOverlayEvent.ElementType.ALL) return;
        MatrixStack stack = new MatrixStack();
    //#endif
        if (guiControls.isStopped()) return;
        if (settingsRegistry.get(Setting.INDICATOR)) {
            FontRenderer fontRenderer = mc.fontRendererObj;
            String text = guiControls.isPaused() ? I18n.format("replaymod.gui.paused") : I18n.format("replaymod.gui.recording");
            fontRenderer.drawString(
                    //#if MC>=11600
                    //$$ stack,
                    //#endif
                    text.toUpperCase(), 30, 18 - (fontRenderer.FONT_HEIGHT / 2), 0xffffffff);
            bindTexture(TEXTURE);
            enableAlpha();
            GuiRenderer renderer = new MinecraftGuiRenderer(stack, MCVer.newScaledResolution(mc));
            renderer.drawTexturedRect(10, 10, 58, 20, 16, 16, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }
}
