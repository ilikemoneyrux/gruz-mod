package pl.gruzclient.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.cosmetics.CapeTextures;
import pl.gruzclient.cosmetics.CosmeticSet;
import pl.gruzclient.cosmetics.Cosmetics;

/** Peleryny Gruz: podmieniamy teksturę peleryny (i elytry) gracza na animowaną. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true, require = 0)
    private void gruz$cape(CallbackInfoReturnable<SkinTextures> cir) {
        CosmeticSet set = Cosmetics.of((AbstractClientPlayerEntity) (Object) this);
        if (!set.hasCape()) return;
        SkinTextures t = cir.getReturnValue();
        Identifier cape = CapeTextures.id(set.cape(), set.color());
        cir.setReturnValue(new SkinTextures(t.texture(), t.textureUrl(), cape, cape, t.model(), t.secure()));
    }
}
