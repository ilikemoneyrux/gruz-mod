package pl.gruzclient.mixin;

import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.scoreboard.number.NumberFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Ukrywanie czerwonych liczb po prawej stronie tablicy wyników */
@Mixin(ScoreboardObjective.class)
public abstract class ScoreboardObjectiveMixin {
    @Inject(method = "getNumberFormatOr", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$hideNumbers(NumberFormat format, CallbackInfoReturnable<NumberFormat> cir) {
        if (Modules.SCOREBOARD.hideNumbers()) cir.setReturnValue(BlankNumberFormat.INSTANCE);
    }
}
