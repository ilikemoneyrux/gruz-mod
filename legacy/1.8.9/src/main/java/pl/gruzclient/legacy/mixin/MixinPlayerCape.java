package pl.gruzclient.legacy.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.legacy.Capes;

/** Peleryny Gruz: gracz z kosmetykami Gruz dostaje naszą teksturę peleryny zamiast tej z Mojanga */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinPlayerCape {
    // Uwaga: w nazwach yarn dla 1.8.9 te dwie metody są zamienione – getSkinId() zwraca teksturę PELERYNY
    // (używa jej CapeFeatureRenderer), a getCapeId() teksturę skina. Wpinamy się więc w getSkinId().
    @Inject(method = "getSkinId()Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    private void gruz$cape(CallbackInfoReturnable<Identifier> cir) {
        Identifier id = Capes.forPlayer((AbstractClientPlayerEntity) (Object) this);
        if (id != null) cir.setReturnValue(id);
    }

    @Inject(method = "canRenderCapeTexture", at = @At("HEAD"), cancellable = true)
    private void gruz$hasCape(CallbackInfoReturnable<Boolean> cir) {
        if (Capes.forPlayer((AbstractClientPlayerEntity) (Object) this) != null) cir.setReturnValue(true);
    }
}
