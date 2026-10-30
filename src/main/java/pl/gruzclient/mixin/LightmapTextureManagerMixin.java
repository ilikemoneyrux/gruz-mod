package pl.gruzclient.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.gruzclient.module.Modules;

/** Fullbright: oświetlenie liczone tak, jakby gracz miał widzenie w ciemności. */
@Mixin(LightTexture.class)
public abstract class LightmapTextureManagerMixin {
    @Redirect(method = "update", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"))
    private boolean gruz$nightVision(LocalPlayer player, Holder<MobEffect> effect) {
        if (Modules.FULLBRIGHT.isEnabled() && effect == MobEffects.NIGHT_VISION) return true;
        return player.hasEffect(effect);
    }
}
