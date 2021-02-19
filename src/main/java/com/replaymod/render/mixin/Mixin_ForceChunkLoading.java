package com.replaymod.render.mixin;

//#if MC>=10800
import com.replaymod.compat.shaders.ShaderReflection;
import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11500
//$$ import java.util.Set;
//#else
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
//#endif

//#if MC>=11400 && MC<11500
//$$ import net.minecraft.client.renderer.ActiveRenderInfo;
//#endif

//#if MC>=11400
//$$ import net.minecraft.client.renderer.WorldRenderer;
//#else
import net.minecraft.client.renderer.RenderGlobal;
//#endif

//#if MC<10904
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

//#if MC>=11400
//$$ @Mixin(WorldRenderer.class)
//#else
@Mixin(RenderGlobal.class)
//#endif
public abstract class Mixin_ForceChunkLoading {
    public ChunkLoadingRenderGlobal replayModRender_hook;

    //#if MC>=11500
    //$$ @Shadow private Set<ChunkBuilder.BuiltChunk> chunksToRebuild;
    //$$
    //$$ @Shadow private ChunkBuilder chunkBuilder;
    //$$
    //$$ @Shadow private boolean needsTerrainUpdate;
    //$$
    //$$ @Shadow public abstract void scheduleTerrainUpdate();
    //$$
    //$$ @Shadow protected abstract void setupTerrain(Camera camera_1, Frustum frustum_1, boolean boolean_1, int int_1, boolean boolean_2);
    //$$
    //$$ @Shadow private int frame;
    //$$
    //$$ private boolean passThrough;
    //$$ @Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true)
    //$$ private void forceAllChunks(Camera camera_1, Frustum frustum_1, boolean boolean_1, int int_1, boolean boolean_2, CallbackInfo ci) throws IllegalAccessException {
    //$$     if (replayModRender_hook == null) {
    //$$         return;
    //$$     }
    //$$     if (passThrough) {
    //$$         return;
    //$$     }
    //$$     if (ShaderReflection.shaders_isShadowPass != null && (boolean) ShaderReflection.shaders_isShadowPass.get(null)) {
    //$$         return;
    //$$     }
    //$$     ci.cancel();
    //$$
    //$$     passThrough = true;
    //$$     try {
    //$$         do {
    //$$             // Determine which chunks shall be visible
    //$$             setupTerrain(camera_1, frustum_1, boolean_1, frame++, boolean_2);
    //$$
    //$$             // Schedule all chunks which need rebuilding (we schedule even important rebuilds because we wait for
    //$$             // all of them anyway and this way we can take advantage of threading)
    //$$             for (ChunkBuilder.BuiltChunk builtChunk : this.chunksToRebuild) {
    //$$                 // MC sometimes schedules invalid chunks when you're outside of loaded chunks (e.g. y > 256)
    //$$                 if (builtChunk.shouldBuild()) {
    //$$                     builtChunk.scheduleRebuild(this.chunkBuilder);
    //$$                 }
    //$$                 builtChunk.cancelRebuild();
    //$$             }
    //$$             this.chunksToRebuild.clear();
    //$$
    //$$             // Upload all chunks
    //$$             this.needsTerrainUpdate |= ((ChunkLoadingRenderGlobal.IBlockOnChunkRebuilds) this.chunkBuilder).uploadEverythingBlocking();
    //$$
    //$$             // Repeat until no more updates are needed
    //$$         } while (this.needsTerrainUpdate);
    //$$     } finally {
    //$$         passThrough = false;
    //$$     }
    //$$ }
    //#else
    private boolean replayModRender_passThroughSetupTerrain;

    @Shadow
    public boolean displayListEntitiesDirty;

    @Shadow
    public ChunkRenderDispatcher renderDispatcher;

    @Shadow
    public abstract void setupTerrain(
            //#if MC>=11400
            //$$ ActiveRenderInfo viewEntity,
            //#else
            Entity viewEntity,
            //#if MC>=11400
            //$$ float partialTicks,
            //#else
            double partialTicks,
            //#endif
            //#endif
            ICamera camera,
            int frameCount,
            boolean playerSpectator
    );

    @Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true)
    private void replayModRender_setupTerrain(
            //#if MC>=11400
            //$$ ActiveRenderInfo viewEntity,
            //#else
            Entity viewEntity,
            //#if MC>=11400
            //$$ float partialTicks,
            //#else
            double partialTicks,
            //#endif
            //#endif
            ICamera camera,
            int frameCount,
            boolean playerSpectator,
            CallbackInfo ci
    ) throws IllegalAccessException {
        if (ShaderReflection.shaders_isShadowPass != null && (boolean) ShaderReflection.shaders_isShadowPass.get(null)) {
            return;
        }
        if (replayModRender_hook != null && !replayModRender_passThroughSetupTerrain) {
            replayModRender_passThroughSetupTerrain = true;

            do {
                setupTerrain(
                        viewEntity,
                        //#if MC<11400
                        partialTicks,
                        //#endif
                        camera,
                        replayModRender_hook.nextFrameId(),
                        playerSpectator
                );
                replayModRender_hook.updateChunks();
            } while (this.displayListEntitiesDirty);

            this.displayListEntitiesDirty = true;

            replayModRender_passThroughSetupTerrain = false;
            ci.cancel();
        }
    }

    //#if MC<10904
    @Inject(method = "isPositionInRenderChunk", at = @At("HEAD"), cancellable = true)
    public void replayModRender_isPositionInRenderChunk(BlockPos pos, RenderChunk chunk, CallbackInfoReturnable<Boolean> ci) {
        if (replayModRender_hook != null) {
            ci.setReturnValue(true);
        }
    }
    //#endif

    @Inject(method = "updateChunks", at = @At("HEAD"), cancellable = true)
    public void replayModRender_updateChunks(long finishTimeNano, CallbackInfo ci) {
        if (replayModRender_hook != null) {
            replayModRender_hook.updateChunks();
            ci.cancel();
        }
    }

    // Prior to 1.9.4, MC always uses the same ChunkRenderDispatcher instance
    //#if MC>=10904
    //$$ @Inject(method = "setWorldAndLoadRenderers(Lnet/minecraft/client/multiplayer/WorldClient;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;stopWorkerThreads()V"))
    //$$ private void stopWorkerThreadsAndChunkLoadingRenderGlobal(CallbackInfo ci) {
    //$$     if (replayModRender_hook != null) {
    //$$         replayModRender_hook.updateRenderDispatcher(null);
    //$$     }
    //$$ }
    //#endif

    @Inject(method = "loadRenderers", at = @At(value = "RETURN"))
    private void setupChunkLoadingRenderGlobal(CallbackInfo ci) {
        if (replayModRender_hook != null) {
            replayModRender_hook.updateRenderDispatcher(this.renderDispatcher);
        }
    }
    //#endif
}
//#endif
