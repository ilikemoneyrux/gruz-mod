//#only MC >= 1.21.11
package pl.gruzclient.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
//#if MC >= 26.1
import net.minecraft.client.renderer.state.level.SkyRenderState;
//#else
import net.minecraft.client.renderer.state.SkyRenderState;
//#endif
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;
import pl.gruzclient.render.GruzDraw;

/**
 * Kosmiczne niebo od 1.21.11: kolor nieba i gwiazdy liczy teraz SkyRenderer (atrybuty środowiska),
 * a nie ClientLevel.getSkyColor – podmieniamy gotowy stan nieba przed rysowaniem.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void gruz$cosmic(ClientLevel level, float tickDelta, Camera camera, SkyRenderState state, CallbackInfo ci) {
        if (!Modules.WORLD.cosmic() || state.skybox != DimensionType.Skybox.OVERWORLD) return;
        int c = GruzDraw.mix(GruzDraw.NEON, 0x000000, 0.86f);
        //#if MC >= 26.3
        state.skyColor = new org.joml.Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
        //#else
        state.skyColor = 0xFF000000 | c;
        //#endif
        state.starBrightness = 1.0f;
    }
}
