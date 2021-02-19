package com.replaymod.render.mixin;

//#if MC>=10800 && MC<11500

import com.replaymod.core.versions.MCVer;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkRenderWorker.class)
public abstract class MixinChunkRenderWorker implements MCVer.ChunkRenderWorkerAccessor {
    @Shadow abstract void processTask(ChunkCompileTaskGenerator task) throws InterruptedException;

    public void doRunTask(ChunkCompileTaskGenerator task) throws InterruptedException {
        processTask(task);
    }
}
//#endif
