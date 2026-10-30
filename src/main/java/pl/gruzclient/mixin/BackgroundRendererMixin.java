package pl.gruzclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;
import pl.gruzclient.render.GruzDraw;

/** Kosmiczne niebo: mgła na horyzoncie w kolorze nieba, żeby świat płynnie w nie przechodził. */
@Mixin(FogRenderer.class)
public abstract class BackgroundRendererMixin {
    @Shadow private static float red;
    @Shadow private static float green;
    @Shadow private static float blue;

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private static void gruz$fog(Camera camera, float tickDelta, ClientLevel world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        if (!Modules.WORLD.cosmic() || camera.getFluidInCamera() != FogType.NONE) return;
        if (!world.dimensionType().hasSkyLight()) return;
        int c = GruzDraw.mix(GruzDraw.NEON, 0x000000, 0.8f);
        red = ((c >> 16) & 255) / 255f;
        green = ((c >> 8) & 255) / 255f;
        blue = (c & 255) / 255f;
        RenderSystem.clearColor(red, green, blue, 0f);
    }
}
