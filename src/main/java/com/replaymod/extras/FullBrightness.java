package com.replaymod.extras;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayOpenedCallback;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import de.johni0702.minecraft.gui.element.GuiImage;
import de.johni0702.minecraft.gui.element.IGuiImage;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.Potion;

//#if FABRIC>=1
//$$ import com.replaymod.core.events.PreRenderCallback;
//$$ import com.replaymod.core.events.PostRenderCallback;
//#else
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
//#endif

public class FullBrightness extends EventRegistrations implements Extra {
    private ReplayMod core;
    private ReplayModReplay module;

    private final IGuiImage indicator = new GuiImage().setTexture(ReplayMod.TEXTURE, 90, 20, 19, 16).setSize(19, 16);

    private Minecraft mc;
    private boolean active;
    //#if MC>=11400
    //$$ private double originalGamma;
    //#else
    private float originalGamma;
    //#endif

    @Override
    public void register(final ReplayMod mod) throws Exception {
        this.core = mod;
        this.module = ReplayModReplay.instance;
        this.mc = mod.getMinecraft();

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.lighting", Keyboard.KEY_Z, new Runnable() {
            @Override
            public void run() {
                active = !active;
                // need to tick once to update lightmap when replay is paused
                //#if MC>=11400
                //$$ mod.getMinecraft().gameRenderer.tick();
                //#else
                mod.getMinecraft().entityRenderer.updateRenderer();
                //#endif
                ReplayHandler replayHandler = module.getReplayHandler();
                if (replayHandler != null) {
                    updateIndicator(replayHandler.getOverlay());
                }
            }
        }, true);

        register();
    }

    public Type getType() {
        String str = core.getSettingsRegistry().get(Setting.FULL_BRIGHTNESS);
        for (Type type : Type.values()) {
            if (type.toString().equals(str)) {
                return type;
            }
        }
        return Type.Gamma;
    }

    //#if FABRIC>=1
    //$$ { on(PreRenderCallback.EVENT, this::preRender); }
    //$$ private void preRender() {
    //#else
    @SubscribeEvent
    public void preRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
    //#endif
        if (active && module.getReplayHandler() != null) {
            Type type = getType();
            if (type == Type.Gamma || type == Type.Both) {
                originalGamma = mc.gameSettings.gammaSetting;
                mc.gameSettings.gammaSetting = 1000;
            }
            if (type == Type.NightVision || type == Type.Both) {
                if (mc.thePlayer != null) {
                    mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision
                            //#if MC<=10809
                            .id
                            //#endif
                            , Integer.MAX_VALUE));
                }
            }
        }
    }

    //#if FABRIC>=1
    //$$ { on(PostRenderCallback.EVENT, this::postRender); }
    //$$ private void postRender() {
    //#else
    @SubscribeEvent
    public void postRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    //#endif
        if (active && module.getReplayHandler() != null) {
            Type type = getType();
            if (type == Type.Gamma || type == Type.Both) {
                mc.gameSettings.gammaSetting = originalGamma;
            }
            if (type == Type.NightVision || type == Type.Both) {
                if (mc.thePlayer != null) {
                    mc.thePlayer.removePotionEffect(Potion.nightVision
                            //#if MC<=10809
                            .id
                            //#endif
                    );
                }
            }
        }
    }

    { on(ReplayOpenedCallback.EVENT, replayHandler -> updateIndicator(replayHandler.getOverlay())); }
    private void updateIndicator(GuiReplayOverlay overlay) {
        if (active) {
            overlay.statusIndicatorPanel.addElements(new HorizontalLayout.Data(1), indicator);
        } else {
            overlay.statusIndicatorPanel.removeElement(indicator);
        }
    }

    enum Type {
        Gamma,
        NightVision,
        Both,
        ;

        @Override
        public String toString() {
            return "replaymod.gui.settings.fullbrightness." + name().toLowerCase();
        }
    }
}
