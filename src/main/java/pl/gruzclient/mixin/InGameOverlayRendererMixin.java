package pl.gruzclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

/** Mniejszy ogień: przesuwamy nakładkę ognia w dół ekranu. */
@Mixin(ScreenEffectRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), require = 0)
    private static void gruz$fireDown(Minecraft client, PoseStack matrices, CallbackInfo ci) {
        matrices.pushPose();
        matrices.translate(0f, -Modules.LOW_FIRE.fireOffset(), 0f);
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"), require = 0)
    private static void gruz$fireRestore(Minecraft client, PoseStack matrices, CallbackInfo ci) {
        matrices.popPose();
    }
}
