package pl.gruzclient.mixin;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.cosmetics.Cosmetics;
import pl.gruzclient.cosmetics.Emote;

/** Emotki: po vanillowej animacji nakładamy pozę emotki (rękawy i czapka idą za nią). */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin<T extends LivingEntity> {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void gruz$emote(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity)) return;
        Emote.Playing play = Cosmetics.EMOTES.get(entity.getUuid());
        if (play == null || play.finished()) return;
        play.emote().apply((BipedEntityModel<?>) (Object) this, play);
    }
}
