//#only MC >= 1.21.11
package pl.gruzclient.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//#if MC >= 26.1
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif
import pl.gruzclient.module.Modules;
import pl.gruzclient.render.GruzDraw;

/** Kosmiczne niebo od 1.21.11: mgła na horyzoncie w kolorze nieba, żeby świat płynnie w nie przechodził. */
@Mixin(FogRenderer.class)
public abstract class FogColorMixin {
    //#if MC >= 26.1
    @Inject(method = "computeFogColor", at = @At("TAIL"), require = 0)
    private void gruz$fog(Camera camera, float tickDelta, ClientLevel level, int renderDistance, float darken, Vector4f out, CallbackInfo ci) {
        gruz$apply(camera, level, out);
    }
    //#else
    @Inject(method = "computeFogColor", at = @At("RETURN"), require = 0)
    private void gruz$fog(Camera camera, float tickDelta, ClientLevel level, int renderDistance, float darken, CallbackInfoReturnable<Vector4f> cir) {
        gruz$apply(camera, level, cir.getReturnValue());
    }
    //#endif

    private static void gruz$apply(Camera camera, ClientLevel level, Vector4f out) {
        if (out == null || !Modules.WORLD.cosmic() || camera.getFluidInCamera() != FogType.NONE) return;
        if (!level.dimensionType().hasSkyLight()) return;
        int c = GruzDraw.mix(GruzDraw.NEON, 0x000000, 0.8f);
        out.set(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, out.w);
    }
}
