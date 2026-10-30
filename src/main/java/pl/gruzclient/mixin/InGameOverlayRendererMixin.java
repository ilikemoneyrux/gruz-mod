package pl.gruzclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

/** Mniejszy ogień: przesuwamy nakładkę ognia w dół ekranu. */
@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), require = 0)
    private static void gruz$fireDown(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        matrices.push();
        matrices.translate(0f, -Modules.LOW_FIRE.fireOffset(), 0f);
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"), require = 0)
    private static void gruz$fireRestore(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        matrices.pop();
    }
}
