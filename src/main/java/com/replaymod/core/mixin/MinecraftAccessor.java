package com.replaymod.core.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Queue;

//#if MC>=11400
//$$ import java.util.concurrent.CompletableFuture;
//#endif

//#if MC<11400
import java.util.concurrent.FutureTask;
//#endif

//#if MC<11400
import net.minecraft.client.resources.IResourcePack;
import java.util.List;
//#endif

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor
    Timer getTimer();
    @Accessor
    void setTimer(Timer value);

    //#if MC>=11400
    //$$ @Accessor("field_213276_aV")
    //$$ CompletableFuture<Void> getResourceReloadFuture();
    //$$ @Accessor("field_213276_aV")
    //$$ void setResourceReloadFuture(CompletableFuture<Void> value);
    //#endif

    //#if MC>=11400
    //$$ @Accessor("field_213275_aU")
    //$$ Queue<Runnable> getRenderTaskQueue();
    //#else
    @Accessor
    Queue<FutureTask<?>> getScheduledTasks();
    //#endif

    @Accessor
    CrashReport getCrashReporter();

    //#if MC<11400
    @Accessor
    List<IResourcePack> getDefaultResourcePacks();
    //#endif
}
