//#if MC>=10800 && MC<11500
package com.replaymod.render.mixin;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
//#if MC>=10904
//$$ import java.util.concurrent.PriorityBlockingQueue;
//#else
import java.util.concurrent.BlockingQueue;
//#endif

@Mixin(ChunkRenderDispatcher.class)
public interface ChunkRenderDispatcherAccessor {
    @Accessor
    List<ChunkRenderWorker> getListThreadedWorkers();

    //#if MC>=10904
    //$$ @Accessor
    //$$ PriorityBlockingQueue<ChunkCompileTaskGenerator> getQueueChunkUpdates();
    //$$ @Accessor
    //$$ void setQueueChunkUpdates(PriorityBlockingQueue<ChunkCompileTaskGenerator> value);
    //#else
    @Accessor
    BlockingQueue<ChunkCompileTaskGenerator> getQueueChunkUpdates();
    @Accessor
    void setQueueChunkUpdates(BlockingQueue<ChunkCompileTaskGenerator> value);
    //#endif
}
//#endif
