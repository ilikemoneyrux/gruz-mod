//#only MC >= 26.1
package pl.gruzclient.mixin;

import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

/**
 * Fullbright od 26.1: oświetlenie liczy się z gotowego stanu (LightmapRenderState), a nie w LightTexture.
 * Podmieniamy go tuż przed wysłaniem do karty (Lightmap.render), a nie w extract – mody typu BadOptimizations
 * pomijają extract, gdy „nic się nie zmieniło”, i wtedy fullbright nie działał. Przełączenie wymusza odświeżenie.
 */
@Mixin(Lightmap.class)
public abstract class LightmapStateMixin {
    @Unique private static boolean gruz$was;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void gruz$fullbright(LightmapRenderState state, CallbackInfo ci) {
        boolean on = Modules.FULLBRIGHT.isEnabled();
        if (on != gruz$was) state.needsUpdate = true; // włączenie/wyłączenie = od razu nowa lightmapa
        gruz$was = on;
        if (on && state.needsUpdate) {
            state.nightVisionEffectIntensity = 1f;
            state.nightVisionColor = LightmapRenderStateExtractor.WHITE;
        }
    }
}
