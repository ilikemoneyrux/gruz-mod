package pl.gruzclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
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
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private boolean reloading;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;

    /** Czarne tło zamiast czerwonego (opcja "logo monochromatyczne"), żeby wygaszanie było czyste */
    @Inject(method = "render", at = @At("HEAD"))
    private void gruz$blackBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        SimpleOption<Boolean> mono = this.client.options.getMonochromeLogo();
        if (!mono.getValue()) mono.setValue(true);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void gruz$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        long now = Util.getMeasuringTimeMs();
        float fade = this.reloadCompleteTime > -1L ? (now - this.reloadCompleteTime) / 1000f : -1f;
        float start = this.reloadStartTime > -1L ? (now - this.reloadStartTime) / 500f : -1f;
        float alpha;
        if (fade >= 1f) alpha = 1f - MathHelper.clamp(fade - 1f, 0f, 1f);
        else if (this.reloading) alpha = MathHelper.clamp(start, 0f, 1f);
        else alpha = 1f;
        float barAlpha = 1f - MathHelper.clamp(fade, 0f, 1f);
        GruzSplash.render(context, alpha, barAlpha, this.progress);
    }
}
