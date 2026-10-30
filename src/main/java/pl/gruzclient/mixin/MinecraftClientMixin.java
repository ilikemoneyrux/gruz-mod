package pl.gruzclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.combat.Combat;
import pl.gruzclient.module.Modules;
import pl.gruzclient.network.GruzNetwork;
import pl.gruzclient.screen.GruzTitleScreen;
import pl.gruzclient.status.GameStatus;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    /** Za każdym razem, gdy gra chce pokazać menu główne, pokazujemy menu Gruz. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen gruz$replaceTitleScreen(Screen screen) {
        if (screen instanceof TitleScreen) return new GruzTitleScreen();
        return screen;
    }

    /** Logika modułów, walka i status dla Discorda – 20 razy na sekundę */
    @Inject(method = "tick", at = @At("TAIL"))
    private void gruz$tick(CallbackInfo ci) {
        pl.gruzclient.render.WindowIcon.apply();
        Modules.tick();
        Combat.tick();
        GameStatus.tick();
        GruzNetwork.tick();
    }

    /** Nowy świat / serwer – czyścimy cel i combo */
    @Inject(method = "joinWorld", at = @At("HEAD"), require = 0)
    private void gruz$joinWorld(CallbackInfo ci) {
        Combat.reset();
        Modules.FREECAM.stop();
    }

    /** Podświetlenie członków drużyny (obrys widoczny przez ściany) */
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$teamGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.NAMETAGS.glow(entity)) cir.setReturnValue(true);
    }

    /** Freecam: w trakcie nie można bić, kopać ani używać przedmiotów */
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$freecamAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.FREECAM.isActive()) cir.setReturnValue(false);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$freecamUse(CallbackInfo ci) {
        if (Modules.FREECAM.isActive()) ci.cancel();
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$freecamBreak(boolean breaking, CallbackInfo ci) {
        if (Modules.FREECAM.isActive()) ci.cancel();
    }
}
