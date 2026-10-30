package pl.gruzclient.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

/** Łapie rozmiar tablicy wyników (z jej teł), żeby edytor HUD wiedział, gdzie ją narysować i przeciągać */
@Mixin(GuiGraphics.class)
public abstract class DrawContextMixin {
    @Inject(method = "fill(IIIIII)V", at = @At("HEAD"), require = 0)
    private void gruz$captureScoreboard(int x1, int y1, int x2, int y2, int z, int color, CallbackInfo ci) {
        if (Modules.SCOREBOARD.drawing) Modules.SCOREBOARD.capture(x1, y1, x2, y2);
    }
}
