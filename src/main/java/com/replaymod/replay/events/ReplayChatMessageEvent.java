//#if FABRIC<=0
package com.replaymod.replay.events;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class ReplayChatMessageEvent extends Event {
    private final CameraEntity cameraEntity;

    public ReplayChatMessageEvent(CameraEntity  cameraEntity) {
        this.cameraEntity = cameraEntity;
    }

    public CameraEntity getCameraEntity() {
        return cameraEntity;
    }
}
//#endif
