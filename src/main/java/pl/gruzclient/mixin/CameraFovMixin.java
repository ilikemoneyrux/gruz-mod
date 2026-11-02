//#only MC >= 26.1
package pl.gruzclient.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Zoom (26.1+): pole widzenia liczy kamera w calculateFov() i zapisuje do pola fov, z którego
 *  bierze je projekcja. getFov() to tylko getter, więc zmiana tam nic nie dawała. */
@Mixin(Camera.class)
public abstract class CameraFovMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void gruz$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        float f = Modules.ZOOM.factor();
        if (f > 1.001f) cir.setReturnValue(cir.getReturnValue() / f);
    }
}
