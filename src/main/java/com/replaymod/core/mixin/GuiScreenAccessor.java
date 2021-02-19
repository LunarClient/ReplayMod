package com.replaymod.core.mixin;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

//#if MC>=11400
//$$ import net.minecraft.client.gui.widget.Widget;
//#else
import net.minecraft.client.gui.GuiButton;
//#endif

//#if MC>=11400
//$$ import net.minecraft.client.gui.IGuiEventListener;
//#endif

@Mixin(GuiScreen.class)
public interface GuiScreenAccessor {
    //#if MC>=11400
    //$$ @Accessor
    //$$ List<Widget> getButtons();
    //#else
    @Accessor("buttonList")
    List<GuiButton> getButtons();
    //#endif

    //#if MC>=11400
    //$$ @Accessor
    //$$ List<IGuiEventListener> getChildren();
    //#endif
}
