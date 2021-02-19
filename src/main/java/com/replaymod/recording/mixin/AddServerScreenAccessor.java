package com.replaymod.recording.mixin;

import net.minecraft.client.gui.GuiScreenAddServer;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiScreenAddServer.class)
public interface AddServerScreenAccessor {
    @Accessor("serverData")
    ServerData getServer();
}
