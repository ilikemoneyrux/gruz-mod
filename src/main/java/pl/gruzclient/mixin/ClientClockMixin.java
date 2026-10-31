//#only MC >= 26.1
package pl.gruzclient.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Stała pora dnia widziana tylko przez Ciebie (od 26.1 czas świata liczą "zegary"). */
@Mixin(net.minecraft.world.level.Level.class)
public abstract class ClientClockMixin {
    @Inject(method = {"getDefaultClockTime", "getOverworldClockTime"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$time(CallbackInfoReturnable<Long> cir) {
        if (!((Object) this instanceof ClientLevel)) return;
        long t = Modules.WORLD.fixedTime();
        if (t >= 0) cir.setReturnValue(t);
    }
}
