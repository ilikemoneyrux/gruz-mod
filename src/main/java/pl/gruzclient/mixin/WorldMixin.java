package pl.gruzclient.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Bez deszczu i burzy (tylko po stronie klienta – serwer nic nie wie). */
@Mixin(World.class)
public abstract class WorldMixin {
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld && Modules.WORLD.noRain()) cir.setReturnValue(0f);
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld && Modules.WORLD.noRain()) cir.setReturnValue(0f);
    }
}
