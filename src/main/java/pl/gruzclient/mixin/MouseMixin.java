package pl.gruzclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.impl.CpsModule;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    /** Liczenie kliknięć do CPS (tylko w trakcie gry, bez otwartego ekranu) */
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void gruz$cps(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS && MinecraftClient.getInstance().currentScreen == null) CpsModule.click(button);
    }

    /** Kółko myszy w trakcie zooma zmienia przybliżenie zamiast slotu */
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void gruz$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen == null && vertical != 0 && Modules.ZOOM.onScroll(vertical)) ci.cancel();
    }
}
