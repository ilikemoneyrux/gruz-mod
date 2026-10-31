package pl.gruzclient.mixin;

import net.minecraft.client.Camera;
//#if MC >= 1.21
import net.minecraft.client.DeltaTracker;
//#endif
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /** Zoom: dzielimy pole widzenia przez aktualny mnożnik */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    //#if MC >= 1.21.2
    private void gruz$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
    //#else
    private void gruz$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
    //#endif
        float f = Modules.ZOOM.factor();
        if (f > 1.001f) cir.setReturnValue(cir.getReturnValue() / f);
    }

    /** Fullbright: pełna siła "widzenia w ciemności" */
    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void gruz$fullbright(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (Modules.FULLBRIGHT.isEnabled()) cir.setReturnValue(1f);
    }

    /** Motion blur: po narysowaniu świata, przed GUI */
    @Inject(method = "render", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    //#if MC >= 1.21
    private void gruz$motionBlur(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        Modules.MOTION_BLUR.render(tickCounter.getGameTimeDeltaPartialTick(true));
    }
    //#else
    private void gruz$motionBlur(float tickDelta, long nanos, boolean tick, CallbackInfo ci) {
        Modules.MOTION_BLUR.render(tickDelta);
    }
    //#endif

    /** Freecam: bez ręki przed kamerą */
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$freecamHand(CallbackInfo ci) {
        if (Modules.FREECAM.isActive()) ci.cancel();
    }
}
