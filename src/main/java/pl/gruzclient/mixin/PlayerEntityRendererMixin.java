package pl.gruzclient.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.cosmetics.CosmeticsFeature;

/** Dokłada warstwę kosmetyków (aureola, korona, pet) do renderera gracza. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    protected PlayerEntityRendererMixin(EntityRendererProvider.Context ctx, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void gruz$addCosmetics(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        this.addLayer(new CosmeticsFeature(this));
    }
}
