package pl.gruzclient.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.gruzclient.module.Modules;

/** Fullbright: oświetlenie liczone tak, jakby gracz miał widzenie w ciemności. */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Redirect(method = "update", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"))
    private boolean gruz$nightVision(ClientPlayerEntity player, RegistryEntry<StatusEffect> effect) {
        if (Modules.FULLBRIGHT.isEnabled() && effect == StatusEffects.NIGHT_VISION) return true;
        return player.hasStatusEffect(effect);
    }
}
