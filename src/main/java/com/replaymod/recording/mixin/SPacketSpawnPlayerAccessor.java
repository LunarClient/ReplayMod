package com.replaymod.recording.mixin;

import net.minecraft.entity.DataWatcher;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(S0CPacketSpawnPlayer.class)
public interface SPacketSpawnPlayerAccessor {
    //#if MC<11500
    @Accessor("watcher")
    DataWatcher getDataManager();
    @Accessor("watcher")
    void setDataManager(DataWatcher value);
    //#endif
}
