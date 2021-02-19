package com.replaymod.render.rendering;

import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.mixin.TimerAccessor;
import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.core.versions.MCVer;
import com.replaymod.pathing.player.AbstractTimelinePlayer;
import com.replaymod.pathing.player.ReplayTimer;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.render.CameraPathExporter;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.ReplayModRender;
import com.replaymod.render.FFmpegWriter;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.events.ReplayRenderCallback;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.gui.GuiRenderingDone;
import com.replaymod.render.gui.GuiVideoRenderer;
import com.replaymod.render.metadata.MetadataInjector;
import com.replaymod.render.mixin.WorldRendererAccessor;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ReportedException;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.util.Timer;

//#if MC>=11600
//$$ import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC>=11500
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//$$ import net.minecraft.client.util.Window;
//$$ import org.lwjgl.opengl.GL11;
//#endif

//#if MC>=11400
//$$ import com.replaymod.render.EXRWriter;
//$$ import com.replaymod.render.mixin.MainWindowAccessor;
//$$ import net.minecraft.client.gui.screen.Screen;
//$$ import org.lwjgl.glfw.GLFW;
//$$ import java.util.concurrent.CompletableFuture;
//#else
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import static com.replaymod.core.versions.MCVer.newScaledResolution;
//#endif

//#if MC>=10800
import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
import static net.minecraft.client.renderer.GlStateManager.*;
//#else
//$$ import com.replaymod.replay.gui.screen.GuiOpeningReplay;
//$$ import static com.replaymod.core.versions.MCVer.GlStateManager.*;
//#endif

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static com.google.common.collect.Iterables.getLast;
import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.render.ReplayModRender.LOGGER;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class VideoRenderer implements RenderInfo {
    private final Minecraft mc = MCVer.getMinecraft();
    private final RenderSettings settings;
    private final ReplayHandler replayHandler;
    private final Timeline timeline;
    private final Pipeline renderingPipeline;
    private final FFmpegWriter ffmpegWriter;
    private final CameraPathExporter cameraPathExporter;

    private int fps;
    private boolean mouseWasGrabbed;
    private boolean debugInfoWasShown;
    private Map<SoundCategory, Float> originalSoundLevels;

    private TimelinePlayer timelinePlayer;
    private Future<Void> timelinePlayerFuture;
    //#if MC>=10800
    private ChunkLoadingRenderGlobal chunkLoadingRenderGlobal;
    //#endif
    //#if MC<10800
    //$$ private GuiOpeningReplay guiOpeningReplay;
    //#endif

    private int framesDone;
    private int totalFrames;

    private final GuiVideoRenderer gui;
    private boolean paused;
    private boolean cancelled;
    private volatile Throwable failureCause;

    private Framebuffer guiFramebuffer;
    private int displayWidth, displayHeight;

    public VideoRenderer(RenderSettings settings, ReplayHandler replayHandler, Timeline timeline) throws IOException {
        this.settings = settings;
        this.replayHandler = replayHandler;
        this.timeline = timeline;
        this.gui = new GuiVideoRenderer(this);
        if (settings.getRenderMethod() == RenderSettings.RenderMethod.BLEND) {
            BlendState.setState(new BlendState(settings.getOutputFile()));

            this.renderingPipeline = Pipelines.newBlendPipeline(this);
            this.ffmpegWriter = null;
        } else {
            FrameConsumer<BitmapFrame> frameConsumer;
            if (settings.getEncodingPreset() == RenderSettings.EncodingPreset.EXR) {
                ffmpegWriter = null;
                //#if MC>=11400
                //$$ frameConsumer = new EXRWriter(settings.getOutputFile().toPath());
                //#else
                throw new UnsupportedOperationException("EXR requires LWJGL3");
                //#endif
            } else {
                frameConsumer = ffmpegWriter = new FFmpegWriter(this);
            }
            FrameConsumer<BitmapFrame> previewingFrameConsumer = new FrameConsumer<BitmapFrame>() {
                @Override
                public void consume(Map<Channel, BitmapFrame> channels) {
                    BitmapFrame bgra = channels.get(Channel.BRGA);
                    if (bgra != null) {
                        gui.updatePreview(bgra.getByteBuffer(), bgra.getSize());
                    }
                    frameConsumer.consume(channels);
                }

                @Override
                public void close() throws IOException {
                    frameConsumer.close();
                }
            };
            this.renderingPipeline = Pipelines.newPipeline(settings.getRenderMethod(), this, previewingFrameConsumer);
        }

        if (settings.isCameraPathExport()) {
            this.cameraPathExporter = new CameraPathExporter(settings);
        } else {
            this.cameraPathExporter = null;
        }
    }

    /**
     * Render this video.
     * @return {@code true} if rendering was successful, {@code false} if the user aborted rendering (or the window was closed)
     */
    public boolean renderVideo() throws Throwable {
        ReplayRenderCallback.Pre.EVENT.invoker().beforeRendering(this);

        setup();

        // Because this might take some time to prepare we'll render the GUI at least once to not confuse the user
        drawGui();

        Timer timer = ((MinecraftAccessor) mc).getTimer();

        // Play up to one second before starting to render
        // This is necessary in order to ensure that all entities have at least two position packets
        // and their first position in the recording is correct.
        // Note that it is impossible to also get the interpolation between their latest position
        // and the one in the recording correct as there's no reliable way to tell when the server ticks
        // or when we should be done with the interpolation of the entity
        Optional<Integer> optionalVideoStartTime = timeline.getValue(TimestampProperty.PROPERTY, 0);
        if (optionalVideoStartTime.isPresent()) {
            int videoStart = optionalVideoStartTime.get();

            if (videoStart > 1000) {
                int replayTime = videoStart - 1000;
                //#if MC>=11200
                //$$ timer.renderPartialTicks = 0;
                //$$ ((TimerAccessor) timer).setTickLength(WrappedTimer.DEFAULT_MS_PER_TICK);
                //#else
                timer.elapsedPartialTicks = timer.renderPartialTicks = 0;
                timer.timerSpeed = 1;
                //#endif
                while (replayTime < videoStart) {
                    //#if MC<11600
                    timer.elapsedTicks = 1;
                    //#endif
                    replayTime += 50;
                    replayHandler.getReplaySender().sendPacketsTill(replayTime);
                    tick();
                }
            }
        }

        //#if MC<11500
        ((WorldRendererAccessor) mc.renderGlobal).setRenderEntitiesStartupCounter(0);
        //#endif

        renderingPipeline.run();

        if (((MinecraftAccessor) mc).getCrashReporter() != null) {
            throw new ReportedException(((MinecraftAccessor) mc).getCrashReporter());
        }

        if (settings.isInjectSphericalMetadata()) {
            MetadataInjector.injectMetadata(settings.getRenderMethod(), settings.getOutputFile(),
                    settings.getTargetVideoWidth(), settings.getTargetVideoHeight(),
                    settings.getSphericalFovX(), settings.getSphericalFovY());
        }

        finish();

        ReplayRenderCallback.Post.EVENT.invoker().afterRendering(this);

        if (failureCause != null) {
            throw failureCause;
        }

        return !cancelled;
    }

    @Override
    public float updateForNextFrame() {
        // because the jGui lib uses Minecraft's displayWidth and displayHeight values, update these temporarily
        //#if MC>=11400
        //$$ int displayWidthBefore = getWindow(mc).getFramebufferWidth();
        //$$ int displayHeightBefore = getWindow(mc).getFramebufferHeight();
        //$$ //noinspection ConstantConditions
        //$$ MainWindowAccessor acc = (MainWindowAccessor) (Object) getWindow(mc);
        //$$ acc.setFramebufferWidth(displayWidth);
        //$$ acc.setFramebufferHeight(displayHeight);
        //#else
        int displayWidthBefore = mc.displayWidth;
        int displayHeightBefore = mc.displayHeight;
        mc.displayWidth = displayWidth;
        mc.displayHeight = displayHeight;
        //#endif

        if (!settings.isHighPerformance() || framesDone % fps == 0) {
            while (drawGui() && paused) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Updating the timer will cause the timeline player to update the game state
        Timer timer = ((MinecraftAccessor) mc).getTimer();
        //#if MC>=11600
        //$$ int elapsedTicks =
        //#endif
        timer.updateTimer(
                //#if MC>=11400
                //$$ MCVer.milliTime()
                //#endif
        );
        //#if MC<11600
        int elapsedTicks = timer.elapsedTicks;
        //#endif

        executeTaskQueue();

        //#if MC<10800
        //$$ if (guiOpeningReplay != null) {
        //$$     guiOpeningReplay.handleInput();
        //$$ }
        //#endif

        while (elapsedTicks-- > 0) {
            tick();
        }

        // change Minecraft's display size back
        //#if MC>=11400
        //$$ acc.setFramebufferWidth(displayWidthBefore);
        //$$ acc.setFramebufferHeight(displayHeightBefore);
        //#else
        mc.displayWidth = displayWidthBefore;
        mc.displayHeight = displayHeightBefore;
        //#endif

        if (cameraPathExporter != null) {
            cameraPathExporter.recordFrame(timer.renderPartialTicks);
        }

        framesDone++;
        return timer.renderPartialTicks;
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }

    private void setup() {
        timelinePlayer = new TimelinePlayer(replayHandler);
        timelinePlayerFuture = timelinePlayer.start(timeline);

        // FBOs are always used in 1.14+
        //#if MC<11400
        if (!OpenGlHelper.isFramebufferEnabled()) {
            Display.setResizable(false);
        }
        //#endif
        if (mc.gameSettings.showDebugInfo) {
            debugInfoWasShown = true;
            mc.gameSettings.showDebugInfo = false;
        }
        //#if MC>=11400
        //$$ if (mc.mouseHelper.isMouseGrabbed()) {
        //$$     mouseWasGrabbed = true;
        //$$ }
        //$$ mc.mouseHelper.ungrabMouse();
        //#else
        if (Mouse.isGrabbed()) {
            mouseWasGrabbed = true;
        }
        Mouse.setGrabbed(false);
        //#endif

        // Mute all sounds except GUI sounds (buttons, etc.)
        originalSoundLevels = new EnumMap<>(SoundCategory.class);
        for (SoundCategory category : SoundCategory.values()) {
            if (category != SoundCategory.MASTER) {
                originalSoundLevels.put(category, mc.gameSettings.getSoundLevel(category));
                mc.gameSettings.setSoundLevel(category, 0);
            }
        }

        fps = settings.getFramesPerSecond();

        long duration = 0;
        for (Path path : timeline.getPaths()) {
            if (!path.isActive()) continue;

            // Prepare path interpolations
            path.updateAll();
            // Find end time
            Collection<Keyframe> keyframes = path.getKeyframes();
            if (keyframes.size() > 0) {
                duration = Math.max(duration, getLast(keyframes).getTime());
            }
        }

        totalFrames = (int) (duration*fps/1000);

        if (cameraPathExporter != null) {
            cameraPathExporter.setup(totalFrames);
        }

        updateDisplaySize();

        //#if MC<=10809
        ScaledResolution scaled = newScaledResolution(mc);
        gui.toMinecraft().setWorldAndResolution(mc, scaled.getScaledWidth(), scaled.getScaledHeight());
        //#endif

        //#if MC>=10800
        chunkLoadingRenderGlobal = new ChunkLoadingRenderGlobal(mc.renderGlobal);
        //#endif

        // Set up our own framebuffer to render the GUI to
        guiFramebuffer = new Framebuffer(displayWidth, displayHeight, true
                //#if MC>=11400
                //$$ , false
                //#endif
        );
    }

    private void finish() {
        if (!timelinePlayerFuture.isDone()) {
            timelinePlayerFuture.cancel(false);
        }
        // Tear down of the timeline player might only happen the next tick after it was cancelled
        timelinePlayer.onTick();

        // FBOs are always used in 1.14+
        //#if MC<11400
        if (!OpenGlHelper.isFramebufferEnabled()) {
            Display.setResizable(true);
        }
        //#endif
        mc.gameSettings.showDebugInfo = debugInfoWasShown;
        if (mouseWasGrabbed) {
            //#if MC>=11400
            //$$ mc.mouseHelper.grabMouse();
            //#else
            mc.mouseHelper.grabMouseCursor();
            //#endif
        }
        for (Map.Entry<SoundCategory, Float> entry : originalSoundLevels.entrySet()) {
            mc.gameSettings.setSoundLevel(entry.getKey(), entry.getValue());
        }
        mc.displayGuiScreen(null);
        //#if MC>=10800
        if (chunkLoadingRenderGlobal != null) {
            chunkLoadingRenderGlobal.uninstall();
        }
        //#endif

        if (!hasFailed() && cameraPathExporter != null) {
            try {
                cameraPathExporter.finish();
            } catch (IOException e) {
                setFailure(e);
            }
        }

        MCVer.playSound(new ResourceLocation("replaymod", "render_success"));

        try {
            if (!hasFailed() && ffmpegWriter != null) {
                new GuiRenderingDone(ReplayModRender.instance, ffmpegWriter.getVideoFile(), totalFrames, settings).display();
            }
        } catch (FFmpegWriter.FFmpegStartupException e) {
            setFailure(e);
        }

        // Finally, resize the Minecraft framebuffer to the actual width/height of the window
        //#if MC>=11400
        //$$ mc.getFramebuffer().func_216491_a(displayWidth, displayHeight
                //#if MC>=11400
                //$$ , false
                //#endif
        //$$ );
        //$$ //noinspection ConstantConditions
        //$$ MainWindowAccessor acc = (MainWindowAccessor) (Object) getWindow(mc);
        //$$ acc.setFramebufferWidth(displayWidth);
        //$$ acc.setFramebufferHeight(displayHeight);
        //#if MC>=11500
        //$$ mc.gameRenderer.onResized(displayWidth, displayHeight);
        //#endif
        //#else
        mc.resize(displayWidth, displayHeight);
        //#endif
    }

    private void executeTaskQueue() {
        //#if MC>=11400
        //$$ while (true) {
        //$$     while (mc.loadingGui != null) {
        //$$         drawGui();
        //$$         ((MinecraftMethodAccessor) mc).replayModExecuteTaskQueue();
        //$$     }
        //$$
        //$$     CompletableFuture<Void> resourceReloadFuture = ((MinecraftAccessor) mc).getResourceReloadFuture();
        //$$     if (resourceReloadFuture != null) {
        //$$         ((MinecraftAccessor) mc).setResourceReloadFuture(null);
        //$$         mc.reloadResources().thenRun(() -> resourceReloadFuture.complete(null));
        //$$         continue;
        //$$     }
        //$$     break;
        //$$ }
        //$$ ((MCVer.MinecraftMethodAccessor) mc).replayModExecuteTaskQueue();
        //#else
        Queue<FutureTask<?>> scheduledTasks = ((MinecraftAccessor) mc).getScheduledTasks();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (scheduledTasks) {
            while (!scheduledTasks.isEmpty()) {
                scheduledTasks.poll().run();
            }
        }
        //#endif

        //#if MC<10800
        //$$ if (mc.currentScreen instanceof GuiOpeningReplay) {
        //$$     guiOpeningReplay = (GuiOpeningReplay) mc.currentScreen;
        //$$ }
        //#endif

        mc.currentScreen = gui.toMinecraft();
    }

    private void tick() {
        //#if MC>=10800 && MC<11400
        try {
            mc.runTick();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //#else
        //$$ mc.runTick();
        //#endif
    }

    public boolean drawGui() {
        do {
            //#if MC>=11400
            //$$ if (GLFW.glfwWindowShouldClose(getWindow(mc).getHandle()) || ((MinecraftAccessor) mc).getCrashReporter() != null) {
            //#else
            if (Display.isCloseRequested() || ((MinecraftAccessor) mc).getCrashReporter() != null) {
            //#endif
                return false;
            }

            // Resize the GUI framebuffer if the display size changed
            if (displaySizeChanged()) {
                updateDisplaySize();
                //#if MC>=11400
                //$$ guiFramebuffer.func_216491_a(displayWidth, displayHeight
                        //#if MC>=11400
                        //$$ , false
                        //#endif
                //$$ );
                //#else
                guiFramebuffer.createBindFramebuffer(mc.displayWidth, mc.displayHeight);
                //#endif
            }

            pushMatrix();
            clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT
                    //#if MC>=11400
                    //$$ , false
                    //#endif
            );
            enableTexture2D();
            guiFramebuffer.bindFramebuffer(true);

            //#if MC>=11500
            //$$ Window window = getWindow(mc);
            //$$ RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
            //$$ RenderSystem.matrixMode(GL11.GL_PROJECTION);
            //$$ RenderSystem.loadIdentity();
            //$$ RenderSystem.ortho(0, window.getFramebufferWidth() / window.getScaleFactor(), window.getFramebufferHeight() / window.getScaleFactor(), 0, 1000, 3000);
            //$$ RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            //$$ RenderSystem.loadIdentity();
            //$$ RenderSystem.translatef(0, 0, -2000);
            //#else
            //#if MC>=11400
            //$$ getWindow(mc).loadGUIRenderMatrix(
                    //#if MC>=11400
                    //$$ false
                    //#endif
            //$$ );
            //#else
            mc.entityRenderer.setupOverlayRendering();
            //#endif
            //#endif

            //#if MC>=11400
            //$$ gui.toMinecraft().init(mc, getWindow(mc).getScaledWidth(), getWindow(mc).getScaledHeight());
            //#else
            ScaledResolution scaled = newScaledResolution(mc);
            gui.toMinecraft().setWorldAndResolution(mc, scaled.getScaledWidth(), scaled.getScaledHeight());
            //#endif

            // Events are polled on 1.13+ in mainWindow.update which is called later
            //#if MC<11400
            //#if MC>=10800
            try {
                gui.toMinecraft().handleInput();
            } catch (IOException e) {
                // That's a strange exception from this kind of method O_o
                // It isn't actually thrown here, so we'll deal with it the easy way
                throw new RuntimeException(e);
            }
            //#else
            //$$ gui.toMinecraft().handleInput();
            //#endif
            //#endif

            //#if MC>=11400
            //$$ int mouseX = (int) mc.mouseHelper.getMouseX() * getWindow(mc).getScaledWidth() / displayWidth;
            //$$ int mouseY = (int) mc.mouseHelper.getMouseY() * getWindow(mc).getScaledHeight() / displayHeight;
            //$$
            //$$ if (mc.loadingGui != null) {
            //$$     Screen orgScreen = mc.currentScreen;
            //$$     try {
            //$$         mc.currentScreen = gui.toMinecraft();
            //$$         mc.loadingGui.render(
                            //#if MC>=11600
                            //$$ new MatrixStack(),
                            //#endif
            //$$                 mouseX, mouseY, 0);
            //$$     } finally {
            //$$         mc.currentScreen = orgScreen;
            //$$     }
            //$$ } else {
            //$$     gui.toMinecraft().tick();
            //$$     gui.toMinecraft().render(
                        //#if MC>=11600
                        //$$ new MatrixStack(),
                        //#endif
            //$$             mouseX, mouseY, 0);
            //$$ }
            //#else
            int mouseX = Mouse.getX() * scaled.getScaledWidth() / mc.displayWidth;
            int mouseY = scaled.getScaledHeight() - Mouse.getY() * scaled.getScaledHeight() / mc.displayHeight - 1;

            gui.toMinecraft().updateScreen();
            gui.toMinecraft().drawScreen(mouseX, mouseY, 0);
            //#endif

            guiFramebuffer.unbindFramebuffer();
            popMatrix();
            pushMatrix();
            guiFramebuffer.framebufferRender(displayWidth, displayHeight);
            popMatrix();

            //#if MC>=11500
            //$$ getWindow(mc).swapBuffers();
            //#else
            //#if MC>=11400
            //$$ getWindow(mc).update(false);
            //#else
            // if not in high performance mode, update the gui size if screen size changed
            // otherwise just swap the progress gui to screen
            if (settings.isHighPerformance()) {
                Display.update();
            } else {
                //#if MC>=10800
                mc.updateDisplay();
                //#else
                //$$ mc.resetSize();
                //#endif
            }
            //#endif
            //#endif
            //#if MC>=11400
            //$$ if (mc.mouseHelper.isMouseGrabbed()) {
            //$$     mc.mouseHelper.ungrabMouse();
            //$$ }
            //#else
            if (Mouse.isGrabbed()) {
                Mouse.setGrabbed(false);
            }
            //#endif

            return !hasFailed() && !cancelled;
        } while (true);
    }

    private boolean displaySizeChanged() {
        //#if MC>=11400
        //$$ int realWidth = getWindow(mc).getWidth();
        //$$ int realHeight = getWindow(mc).getHeight();
        //#else
        int realWidth = Display.getWidth();
        int realHeight = Display.getHeight();
        //#endif
        if (realWidth == 0 || realHeight == 0) {
            // These can be zero on Windows if minimized.
            // Creating zero-sized framebuffers however will throw an error, so we never want to switch to zero values.
            return false;
        }
        return displayWidth != realWidth || displayHeight != realHeight;
    }

    private void updateDisplaySize() {
        //#if MC>=11400
        //$$ displayWidth = getWindow(mc).getWidth();
        //$$ displayHeight = getWindow(mc).getHeight();
        //#else
        displayWidth = Display.getWidth();
        displayHeight = Display.getHeight();
        //#endif
    }

    public int getFramesDone() {
        return framesDone;
    }

    @Override
    public ReadableDimension getFrameSize() {
        return new Dimension(settings.getVideoWidth(), settings.getVideoHeight());
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public int getVideoTime() { return framesDone * 1000 / fps; }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void cancel() {
        if (ffmpegWriter != null) {
            ffmpegWriter.abort();
        }
        this.cancelled = true;
        renderingPipeline.cancel();
    }

    public boolean hasFailed() {
        return failureCause != null;
    }

    public synchronized void setFailure(Throwable cause) {
        if (this.failureCause != null) {
            LOGGER.error("Further failure during failed rendering: ", cause);
        } else {
            LOGGER.error("Failure during rendering: ", cause);
            this.failureCause = cause;
            cancel();
        }
    }

    private class TimelinePlayer extends AbstractTimelinePlayer {
        public TimelinePlayer(ReplayHandler replayHandler) {
            super(replayHandler);
        }

        @Override
        public long getTimePassed() {
            return getVideoTime();
        }
    }

    public static String[] checkCompat() {
        //#if FABRIC>=1
        //$$ if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("sodium")) {
        //$$     return new String[] {
        //$$             "Rendering is not currently supported while Sodium is installed.",
        //$$             "See https://github.com/ReplayMod/ReplayMod/issues/150",
        //$$             "For now, you need to uninstall Sodium before rendering!"
        //$$     };
        //$$ }
        //#endif
        return null;
    }
}
