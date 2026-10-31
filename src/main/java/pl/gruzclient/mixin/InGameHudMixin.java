//#swap MC >= 26.2 | Gui => Hud
//#swap MC >= 26.1 | "render" => "extractRenderState"
//#swap MC >= 26.1 | "renderTitle" => "extractTitle"
//#swap MC >= 26.1 | "renderCrosshair" => "extractCrosshair"
package pl.gruzclient.mixin;

//#if MC >= 1.21
import net.minecraft.client.DeltaTracker;
//#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
    /** Tryb nagrywania: cały HUD znika (ręka zostaje, bo nie jest częścią HUD) */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void gruz$recording(GuiGraphics context,
            //#if MC >= 1.21
            DeltaTracker tickCounter,
            //#else
            float tickCounter,
            //#endif
            CallbackInfo ci) {
        if (Modules.RECORDING.hidingHud()) ci.cancel();
    }

    /** Ostrzeżenie o niskim HP i elementy HUD Gruz rysowane na wierzchu standardowego HUD */
    @Inject(method = "render", at = @At("TAIL"))
    private void gruz$renderHud(GuiGraphics context,
            //#if MC >= 1.21
            DeltaTracker tickCounter,
            //#else
            float tickCounter,
            //#endif
            CallbackInfo ci) {
        Modules.LOW_HP.render(context);
        Modules.renderHud(context);
    }

    /** Tablica wyników: rozmiar / pozycja / ukrycie */
    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$scoreboardHead(GuiGraphics context, Objective objective, CallbackInfo ci) {
        if (Modules.SCOREBOARD.hidden() || editorHidesVanilla()) { ci.cancel(); return; }
        Modules.SCOREBOARD.begin(context);
    }

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("RETURN"), require = 0)
    private void gruz$scoreboardTail(GuiGraphics context, Objective objective, CallbackInfo ci) {
        Modules.SCOREBOARD.end(context);
    }

    //#if MC >= 1.21
    /** Duże napisy na środku ekranu (title / subtitle) */
    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$titleHead(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Modules.HUD_TWEAKS.hideTitles()) { ci.cancel(); return; }
        Modules.HUD_TWEAKS.titleBegin(context);
    }

    @Inject(method = "renderTitle", at = @At("RETURN"), require = 0)
    private void gruz$titleTail(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        Modules.HUD_TWEAKS.titleEnd(context);
    }
    //#endif

    /** W edytorze HUD gra jest rozmyta, więc tablicę i pasek bossa rysuje sam edytor (ostro, na wierzchu) */
    private static boolean editorHidesVanilla() {
        return Minecraft.getInstance().screen instanceof pl.gruzclient.screen.HudEditorScreen
                && !pl.gruzclient.screen.HudEditorScreen.drawingVanilla;
    }

    /** Własny celownik */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void gruz$crosshair(GuiGraphics context,
            //#if MC >= 1.21
            DeltaTracker tickCounter,
            //#endif
            CallbackInfo ci) {
        if (!Modules.CROSSHAIR.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        ci.cancel();
        if (mc.options.getCameraType().isFirstPerson() && !mc.getDebugOverlay().showDebugScreen()) {
            Modules.CROSSHAIR.draw(context);
        }
    }
}
