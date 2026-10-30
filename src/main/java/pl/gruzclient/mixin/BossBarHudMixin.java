package pl.gruzclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

/** Pasek bossa: rozmiar, przesunięcie w dół, ukrycie */
@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$head(DrawContext context, CallbackInfo ci) {
        Modules.HUD_TWEAKS.bossCount = ((BossBarHudAccessor) (Object) this).gruz$getBossBars().size();
        if (Modules.HUD_TWEAKS.hideBossbar()) { ci.cancel(); return; }
        if (net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof pl.gruzclient.screen.HudEditorScreen
                && !pl.gruzclient.screen.HudEditorScreen.drawingVanilla) { ci.cancel(); return; }
        Modules.HUD_TWEAKS.bossBegin(context);
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void gruz$tail(DrawContext context, CallbackInfo ci) {
        Modules.HUD_TWEAKS.bossEnd(context);
    }
}
