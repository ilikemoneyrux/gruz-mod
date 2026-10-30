package pl.gruzclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(InGameHud.class)
public interface InGameHudInvoker {
    @Invoker("renderScoreboardSidebar")
    void gruz$renderScoreboard(DrawContext context, RenderTickCounter tickCounter);
}
