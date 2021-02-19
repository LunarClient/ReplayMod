package com.replaymod.recording.mixin;

import com.replaymod.recording.ServerInfoExt;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerData.class)
public abstract class MixinServerInfo implements ServerInfoExt {
    private Boolean autoRecording;

    @Override
    public Boolean getAutoRecording() {
        return autoRecording;
    }

    @Override
    public void setAutoRecording(Boolean autoRecording) {
        this.autoRecording = autoRecording;
    }

    @Inject(method = "getNBTCompound", at = @At("RETURN"))
    private void serialize(CallbackInfoReturnable<NBTTagCompound> ci) {
        NBTTagCompound tag = ci.getReturnValue();
        if (autoRecording != null) {
            tag.setBoolean("autoRecording", autoRecording);
        }
    }

    @Inject(method = "getServerDataFromNBTCompound", at = @At("RETURN"))
    private static void deserialize(NBTTagCompound tag, CallbackInfoReturnable<ServerData> ci) {
        ServerInfoExt serverInfo = ServerInfoExt.from(ci.getReturnValue());
        if (tag.hasKey("autoRecording")) {
            serverInfo.setAutoRecording(tag.getBoolean("autoRecording"));
        }
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    public void copyFrom(ServerData serverInfo, CallbackInfo ci) {
        ServerInfoExt from = ServerInfoExt.from(serverInfo);
        this.autoRecording = from.getAutoRecording();
    }
}
