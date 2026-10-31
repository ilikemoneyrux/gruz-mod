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
    @Inject(method = "renderFire", at = @At("HEAD"), require = 0)
    //#if MC >= 1.21.9
    private static void gruz$fireDown(PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource buffers, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, CallbackInfo ci) {
    //#elif MC >= 1.21.2
    private static void gruz$fireDown(PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource buffers, CallbackInfo ci) {
    //#else
    private static void gruz$fireDown(Minecraft client, PoseStack matrices, CallbackInfo ci) {
    //#endif
        matrices.pushPose();
        matrices.translate(0f, -Modules.LOW_FIRE.fireOffset(), 0f);
    }

    @Inject(method = "renderFire", at = @At("RETURN"), require = 0)
    //#if MC >= 1.21.9
    private static void gruz$fireRestore(PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource buffers, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, CallbackInfo ci) {
    //#elif MC >= 1.21.2
    private static void gruz$fireRestore(PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource buffers, CallbackInfo ci) {
    //#else
    private static void gruz$fireRestore(Minecraft client, PoseStack matrices, CallbackInfo ci) {
    //#endif
        matrices.popPose();
    }
}
