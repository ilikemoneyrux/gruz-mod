//#swap MC >= 26.1 | "render" => "extractRenderState"
package pl.gruzclient.mixin;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.render.GruzSplash;

/**
 * Podmienia wygląd ekranu ładowania Mojang. Logika wczytywania zostaje oryginalna
 * – rysujemy tylko własną warstwę na wierzchu z tą samą przezroczystością.
 */
@Mixin(LoadingOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private boolean fadeIn;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;

    /** Czarne tło zamiast czerwonego (opcja "logo monochromatyczne"), żeby wygaszanie było czyste */
    @Inject(method = "render", at = @At("HEAD"))
    private void gruz$blackBackground(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        OptionInstance<Boolean> mono = this.minecraft.options.darkMojangStudiosBackground();
        if (!mono.get()) mono.set(true);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void gruz$render(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        long now = Util.getMillis();
        float fade = this.fadeOutStart > -1L ? (now - this.fadeOutStart) / 1000f : -1f;
        float start = this.fadeInStart > -1L ? (now - this.fadeInStart) / 500f : -1f;
        float alpha;
        if (fade >= 1f) alpha = 1f - Mth.clamp(fade - 1f, 0f, 1f);
        else if (this.fadeIn) alpha = Mth.clamp(start, 0f, 1f);
        else alpha = 1f;
        float barAlpha = 1f - Mth.clamp(fade, 0f, 1f);
        GruzSplash.render(context, alpha, barAlpha, this.currentProgress);
    }
}
