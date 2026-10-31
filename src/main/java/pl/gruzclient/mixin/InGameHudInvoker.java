//#only MC >= 1.21
package pl.gruzclient.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface InGameHudInvoker {
    @Invoker("renderScoreboardSidebar")
    void gruz$renderScoreboard(GuiGraphics context, DeltaTracker tickCounter);
}
