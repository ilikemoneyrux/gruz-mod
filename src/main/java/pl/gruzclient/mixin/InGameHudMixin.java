package pl.gruzclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    /** Tryb nagrywania: cały HUD znika (ręka zostaje, bo nie jest częścią HUD) */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void gruz$recording(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Modules.RECORDING.hidingHud()) ci.cancel();
    }

    /** Ostrzeżenie o niskim HP i elementy HUD Gruz rysowane na wierzchu standardowego HUD */
    @Inject(method = "render", at = @At("TAIL"))
    private void gruz$renderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Modules.LOW_HP.render(context);
        Modules.renderHud(context);
    }

    /** Tablica wyników: rozmiar / pozycja / ukrycie */
    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$scoreboardHead(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (Modules.SCOREBOARD.hidden() || editorHidesVanilla()) { ci.cancel(); return; }
        Modules.SCOREBOARD.begin(context);
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("RETURN"), require = 0)
    private void gruz$scoreboardTail(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        Modules.SCOREBOARD.end(context);
    }

    /** Duże napisy na środku ekranu (title / subtitle) */
    @Inject(method = "renderTitleAndSubtitle", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$titleHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Modules.HUD_TWEAKS.hideTitles()) { ci.cancel(); return; }
        Modules.HUD_TWEAKS.titleBegin(context);
    }

    @Inject(method = "renderTitleAndSubtitle", at = @At("RETURN"), require = 0)
    private void gruz$titleTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Modules.HUD_TWEAKS.titleEnd(context);
    }

    /** W edytorze HUD gra jest rozmyta, więc tablicę i pasek bossa rysuje sam edytor (ostro, na wierzchu) */
    private static boolean editorHidesVanilla() {
        return MinecraftClient.getInstance().currentScreen instanceof pl.gruzclient.screen.HudEditorScreen
                && !pl.gruzclient.screen.HudEditorScreen.drawingVanilla;
    }

    /** Własny celownik */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void gruz$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!Modules.CROSSHAIR.isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ci.cancel();
        if (mc.options.getPerspective().isFirstPerson() && !mc.getDebugHud().shouldShowDebugHud()) {
            Modules.CROSSHAIR.draw(context);
        }
    }
}
