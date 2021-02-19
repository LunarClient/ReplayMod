package com.replaymod.core.utils;

import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.network.PacketBuffer;
//#if MC>=10904
//$$ import net.minecraft.util.ResourceLocation;
//#endif

//#if MC<=10710
//$$ import io.netty.buffer.Unpooled;
//#endif

import static com.replaymod.core.versions.MCVer.readString;

/**
 * Restrictions set by the server,
 * @see <a href="https://gist.github.com/Johni0702/2547c463e51f65f312cb">Replay Restrictions Gist</a>
 */
public class Restrictions {
    //#if MC>=11400
    //$$ public static final ResourceLocation PLUGIN_CHANNEL = new ResourceLocation("replaymod", "restrict");
    //#else
    public static final String PLUGIN_CHANNEL = "Replay|Restrict";
    //#endif
    private boolean noXray;
    private boolean noNoclip;
    private boolean onlyFirstPerson;
    private boolean onlyRecordingPlayer;

    public String handle(S3FPacketCustomPayload packet) {
        //#if MC>=10800
        PacketBuffer buffer = packet.getBufferData();
        //#else
        //$$ PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(packet.func_149168_d()));
        //#endif
        while (buffer.isReadable()) {
            String name = readString(buffer, 64);
            boolean active = buffer.readBoolean();
//            if ("no_xray".equals(name)) {
//                noXray = active;
//            } else if ("no_noclip".equals(name)) {
//                noNoclip = active;
//            } else if ("only_first_person".equals(name)) {
//                onlyFirstPerson = active;
//            } else if ("only_recording_player".equals(name)) {
//                onlyRecordingPlayer = active;
//            } else {
                return name;
//            }
        }
        return null;
    }

    public boolean isNoXray() {
        return noXray;
    }

    public boolean isNoNoclip() {
        return noNoclip;
    }

    public boolean isOnlyFirstPerson() {
        return onlyFirstPerson;
    }

    public boolean isOnlyRecordingPlayer() {
        return onlyRecordingPlayer;
    }
}
