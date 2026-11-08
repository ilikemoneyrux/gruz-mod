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

/** Freelook: ruch myszy obraca kamerę, a nie postać */
@Mixin(Entity.class)
public abstract class MixinEntityLook {
    @Inject(method = "increaseTransforms", at = @At("HEAD"), cancellable = true)
    private void gruz$look(float yaw, float pitch, CallbackInfo ci) {
        if (Mods.freelookActive && (Object) this == MinecraftClient.getInstance().player) {
            Mods.look(yaw, pitch);
            ci.cancel();
        }
    }
}
