package pl.gruzclient.legacy.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.legacy.Hud;
import pl.gruzclient.legacy.Mods;

/** CPS: każde wciśnięcie klawisza/przycisku w grze (mysz: -100 lewy, -99 prawy) */
@Mixin(KeyBinding.class)
public abstract class MixinKeyPress {
    @Inject(method = "onKeyPressed", at = @At("HEAD"))
    private static void gruz$press(int keyCode, CallbackInfo ci) { Mods.keyPressed(keyCode); }
}
