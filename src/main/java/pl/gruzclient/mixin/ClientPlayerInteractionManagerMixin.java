package pl.gruzclient.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.combat.Combat;

/** Każdy cios gracza: zasięg, combo, cel, dźwięk trafienia. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"), require = 0)
    private void gruz$onAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        Combat.onAttack(player, target);
    }
}
