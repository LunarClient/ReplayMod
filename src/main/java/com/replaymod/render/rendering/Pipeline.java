package com.replaymod.render.rendering;

import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.capturer.WorldRenderer;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.processor.GlToAbsoluteDepthProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;

//#if MC>=11400
//$$ import org.lwjgl.glfw.GLFW;
//#else
import org.lwjgl.opengl.Display;
//#endif

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.replaymod.core.versions.MCVer.getMinecraft;
//#if MC>=11400
//$$ import static com.replaymod.core.versions.MCVer.getWindow;
//#endif

public class Pipeline<R extends Frame, P extends Frame> implements Runnable {

    private final WorldRenderer worldRenderer;
    private final FrameCapturer<R> capturer;
    private final FrameProcessor<R, P> processor;
    private final GlToAbsoluteDepthProcessor depthProcessor;
    private int consumerNextFrame;
    private final Object consumerLock = new Object();
    private final FrameConsumer<P> consumer;

    private volatile boolean abort;

    public Pipeline(WorldRenderer worldRenderer, FrameCapturer<R> capturer, FrameProcessor<R, P> processor, FrameConsumer<P> consumer) {
        this.worldRenderer = worldRenderer;
        this.capturer = capturer;
        this.processor = processor;
        this.consumer = consumer;

        float near = 0.05f;
        float far = getMinecraft().gameSettings.renderDistanceChunks * 16 * 4;
        this.depthProcessor = new GlToAbsoluteDepthProcessor(near, far);
    }

    @Override
    public synchronized void run() {
        consumerNextFrame = 0;
        int processors = Runtime.getRuntime().availableProcessors();
        int processThreads = Math.max(1, processors - 2); // One processor for the main thread and one for ffmpeg, sorry OS :(
        ExecutorService processService = new ThreadPoolExecutor(processThreads, processThreads,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(2) {
                    @Override
                    public boolean offer(Runnable runnable) {
                        try {
                            put(runnable);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                        return true;
                    }
                }, new ThreadPoolExecutor.DiscardPolicy());

        Minecraft mc = MCVer.getMinecraft();
        while (!capturer.isDone() && !abort) {
            //#if MC>=11400
            //$$ if (GLFW.glfwWindowShouldClose(getWindow(mc).getHandle()) || ((MinecraftAccessor) mc).getCrashReporter() != null) {
            //#else
            if (Display.isCloseRequested() || ((MinecraftAccessor) mc).getCrashReporter() != null) {
            //#endif
                processService.shutdown();
                return;
            }
            Map<Channel, R> rawFrame = capturer.process();
            if (rawFrame != null) {
                processService.submit(new ProcessTask(rawFrame));
            }
        }

        processService.shutdown();
        try {
            processService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            worldRenderer.close();
            capturer.close();
            processor.close();
            consumer.close();
        } catch (Throwable t) {
            CrashReport crashReport = CrashReport.makeCrashReport(t, "Cleaning up rendering pipeline");
            throw new ReportedException(crashReport);
        }
    }

    public void cancel() {
        abort = true;
    }

    private class ProcessTask implements Runnable {
        private final Map<Channel, R> rawChannels;

        public ProcessTask(Map<Channel, R> rawChannels) {
            this.rawChannels = rawChannels;
        }

        @Override
        public void run() {
            try {
                Integer frameId = null;
                Map<Channel, P> processedChannels = new HashMap<>();
                for (Map.Entry<Channel, R> entry : rawChannels.entrySet()) {
                    P processedFrame = processor.process(entry.getValue());
                    if (entry.getKey() == Channel.DEPTH && processedFrame instanceof BitmapFrame) {
                        depthProcessor.process((BitmapFrame) processedFrame);
                    }
                    processedChannels.put(entry.getKey(), processedFrame);
                    frameId = processedFrame.getFrameId();
                }
                if (frameId == null) {
                    return;
                }
                synchronized (consumerLock) {
                    while (consumerNextFrame != frameId) {
                        try {
                            consumerLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    consumer.consume(processedChannels);
                    consumerNextFrame++;
                    consumerLock.notifyAll();
                }
            } catch (Throwable t) {
                CrashReport crashReport = CrashReport.makeCrashReport(t, "Processing frame");
                MCVer.getMinecraft().crashed(crashReport);
            }
        }
    }
}
