package com.replaymod.render.mixin;

import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

//#if MC>=10800
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

//#if MC>=11500
//$$ import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
//#else
import net.minecraft.client.renderer.chunk.RenderChunk;
//#endif
//#endif

@Mixin(RenderGlobal.class)
public interface WorldRendererAccessor {
    //#if MC<11500
    @Accessor
    void setRenderEntitiesStartupCounter(int value);

    //#if MC>=10800
    @Accessor
    ChunkRenderDispatcher getRenderDispatcher();

    @Accessor
    void setDisplayListEntitiesDirty(boolean value);

    @Accessor
    //#if MC>=11500
    //$$ Set<BuiltChunk> getChunksToUpdate();
    //#else
    Set<RenderChunk> getChunksToUpdate();
    //#endif
    //#endif
    //#endif
}
