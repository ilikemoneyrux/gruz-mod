package pl.gruzclient.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.combat.Combat;

/** Każdy cios gracza: zasięg, combo, cel, dźwięk trafienia. */
@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attack", at = @At("HEAD"), require = 0)
    private void gruz$onAttack(Player player, Entity target, CallbackInfo ci) {
        Combat.onAttack(player, target);
    }
}
