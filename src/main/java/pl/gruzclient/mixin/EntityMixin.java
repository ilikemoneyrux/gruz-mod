package pl.gruzclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

/** Freelook: w trakcie trybu ruch myszy obraca kamerę, a nie gracza. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void gruz$freelook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (Modules.FREECAM.isActive() && (Object) this == Minecraft.getInstance().player) {
            Modules.FREECAM.look(cursorDeltaX, cursorDeltaY);
            ci.cancel();
            return;
        }
        if (Modules.FREELOOK.isActive() && (Object) this == Minecraft.getInstance().player) {
            Modules.FREELOOK.look(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
