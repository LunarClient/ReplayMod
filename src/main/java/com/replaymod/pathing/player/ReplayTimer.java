package com.replaymod.pathing.player;

import com.replaymod.core.utils.WrappedTimer;
import de.johni0702.minecraft.gui.utils.Event;
import net.minecraft.util.Timer;

/**
 * Wrapper around the current timer that prevents the timer from advancing by itself.
 */
public class ReplayTimer extends WrappedTimer {
    //#if MC>=11400
    //$$ private final Timer state = new Timer(0, 0);
    //#else
    private final Timer state = new Timer(0);
    //#endif

    //#if MC>=11600
    //$$ public int ticksThisFrame;
    //#endif

    public ReplayTimer(Timer wrapped) {
        super(wrapped);
    }

    @Override
    // This should be handled by Remap but it isn't (was handled before a9724e3).
    //#if MC>=11400
    //$$ public
    //#if MC>=11600
    //$$ int
    //#else
    //$$ void
    //#endif
    //$$ updateTimer(
    //#else
    public void updateTimer(
    //#endif
            //#if MC>=11400
            //$$ long sysClock
            //#endif
    ) {
        copy(this, state); // Save our current state
        try {
            //#if MC>=11600
            //$$ ticksThisFrame =
            //#endif
            wrapped.updateTimer(
                    //#if MC>=11400
                    //$$ sysClock
                    //#endif
            ); // Update current state
        } finally {
            copy(state, this); // Restore our old state
            UpdatedCallback.EVENT.invoker().onUpdate();
        }
        //#if MC>=11600
        //$$ return ticksThisFrame;
        //#endif
    }

    public Timer getWrapped() {
        return wrapped;
    }

    public interface UpdatedCallback {
        Event<UpdatedCallback> EVENT = Event.create((listeners) ->
                () -> {
                    for (UpdatedCallback listener : listeners) {
                        listener.onUpdate();
                    }
                }
        );
        void onUpdate();
    }
}
