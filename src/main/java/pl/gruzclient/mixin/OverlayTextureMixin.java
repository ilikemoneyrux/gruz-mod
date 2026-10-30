package pl.gruzclient.mixin;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.impl.HitColorModule;

/** Hit Color: przekazujemy teksturę "overlay" do modułu, który podmienia kolor obrażeń. */
@Mixin(OverlayTexture.class)
public abstract class OverlayTextureMixin {
    @Shadow @Final private NativeImageBackedTexture texture;

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void gruz$init(CallbackInfo ci) {
        HitColorModule.setTexture(this.texture);
        Modules.HIT_COLOR.apply(true);
    }
}
