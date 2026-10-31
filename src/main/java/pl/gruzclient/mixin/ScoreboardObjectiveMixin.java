//#only MC >= 1.20.3
package pl.gruzclient.mixin;

import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Ukrywanie czerwonych liczb po prawej stronie tablicy wyników */
@Mixin(Objective.class)
public abstract class ScoreboardObjectiveMixin {
    @Inject(method = "numberFormatOrDefault", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$hideNumbers(NumberFormat format, CallbackInfoReturnable<NumberFormat> cir) {
        if (Modules.SCOREBOARD.hideNumbers()) cir.setReturnValue(BlankFormat.INSTANCE);
    }
}
