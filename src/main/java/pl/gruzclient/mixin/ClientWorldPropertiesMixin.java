package pl.gruzclient.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Stała pora dnia widziana tylko przez Ciebie. */
@Mixin(ClientWorld.Properties.class)
public abstract class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$time(CallbackInfoReturnable<Long> cir) {
        long t = Modules.WORLD.fixedTime();
        if (t >= 0) cir.setReturnValue(t);
    }
}
