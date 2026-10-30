package pl.gruzclient.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import pl.gruzclient.module.Modules;

/** Freelook: kamera używa własnego kąta zamiast kąta gracza. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArgs(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0))
    private void gruz$freelookRotation(Args args) {
        if (Modules.FREELOOK.isActive()) {
            args.set(0, Modules.FREELOOK.yaw);
            args.set(1, Modules.FREELOOK.pitch);
        }
    }
}
