package pl.gruzclient.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Bez deszczu i burzy (tylko po stronie klienta – serwer nic nie wie). */
@Mixin(Level.class)
public abstract class WorldMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientLevel && Modules.WORLD.noRain()) cir.setReturnValue(0f);
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientLevel && Modules.WORLD.noRain()) cir.setReturnValue(0f);
    }
}
