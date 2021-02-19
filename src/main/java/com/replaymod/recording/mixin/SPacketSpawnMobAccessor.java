package com.replaymod.recording.mixin;

import net.minecraft.entity.DataWatcher;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(S0FPacketSpawnMob.class)
public interface SPacketSpawnMobAccessor {
    //#if MC<11500
    @Accessor("field_149043_l")
    DataWatcher getDataManager();
    @Accessor("field_149043_l")
    void setDataManager(DataWatcher value);
    //#endif
}
