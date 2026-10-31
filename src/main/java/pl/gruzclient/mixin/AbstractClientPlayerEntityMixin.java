package pl.gruzclient.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
//#if MC >= 1.20.2
import net.minecraft.client.resources.PlayerSkin;
//#endif
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.cosmetics.CapeTextures;
import pl.gruzclient.cosmetics.CosmeticSet;
import pl.gruzclient.cosmetics.Cosmetics;

/** Peleryny Gruz: podmieniamy teksturę peleryny (i elytry) gracza na animowaną. */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerEntityMixin {
    //#if MC >= 1.20.2
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void gruz$cape(CallbackInfoReturnable<PlayerSkin> cir) {
        CosmeticSet set = Cosmetics.of((AbstractClientPlayer) (Object) this);
        if (!set.hasCape()) return;
        PlayerSkin t = cir.getReturnValue();
        ResourceLocation cape = CapeTextures.id(set.cape(), set.color());
        //#if MC >= 1.21.9
        var tex = new net.minecraft.core.ClientAsset.ResourceTexture(cape, cape);
        cir.setReturnValue(new PlayerSkin(t.body(), tex, tex, t.model(), t.secure()));
        //#else
        cir.setReturnValue(new PlayerSkin(t.texture(), t.textureUrl(), cape, cape, t.model(), t.secure()));
        //#endif
    }
    //#else
    // 1.20.1: peleryna i elytra mają osobne metody zwracające teksturę
    @Inject(method = "getCloakTextureLocation", at = @At("RETURN"), cancellable = true, require = 0)
    private void gruz$cape(CallbackInfoReturnable<ResourceLocation> cir) {
        CosmeticSet set = Cosmetics.of((AbstractClientPlayer) (Object) this);
        if (set.hasCape()) cir.setReturnValue(CapeTextures.id(set.cape(), set.color()));
    }

    @Inject(method = "getElytraTextureLocation", at = @At("RETURN"), cancellable = true, require = 0)
    private void gruz$elytra(CallbackInfoReturnable<ResourceLocation> cir) {
        CosmeticSet set = Cosmetics.of((AbstractClientPlayer) (Object) this);
        if (set.hasCape()) cir.setReturnValue(CapeTextures.id(set.cape(), set.color()));
    }
    //#endif
}
