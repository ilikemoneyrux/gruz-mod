package pl.gruzclient.mixin;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Przezroczystość tła tablicy wyników (tylko w trakcie jej rysowania) */
@Mixin(Options.class)
public abstract class GameOptionsMixin {
    @Inject(method = "getBackgroundColor(F)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$scoreboardBg(float fallbackOpacity, CallbackInfoReturnable<Integer> cir) {
        if (Modules.SCOREBOARD.drawing) cir.setReturnValue(Modules.SCOREBOARD.background(fallbackOpacity));
    }
}
