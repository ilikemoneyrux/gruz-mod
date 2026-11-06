package pl.gruzclient.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.cosmetics.Cosmetics;
import pl.gruzclient.cosmetics.Emote;

/** Emotki: po vanillowej animacji nakładamy pozę emotki (rękawy i czapka idą za nią). */
@Mixin(HumanoidModel.class)
//#if MC >= 1.21.2
public abstract class BipedEntityModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"), require = 0)
    private void gruz$emote(net.minecraft.client.renderer.entity.state.HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof net.minecraft.client.renderer.entity.state.PlayerRenderState ps)) return;
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(ps.id) instanceof Player entity)) return;
        Emote.Playing play = Cosmetics.EMOTES.get(entity.getUUID());
        if (play == null || play.finished()) return;
        if (pl.gruzclient.cosmetics.EmoteAnim.handled(entity.getUUID(), play)) return; // gra biblioteka animacji (zginanie)
        play.emote().apply((HumanoidModel<?>) (Object) this, play);
    }
}
//#else
public abstract class BipedEntityModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void gruz$emote(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player)) return;
        Emote.Playing play = Cosmetics.EMOTES.get(entity.getUUID());
        if (play == null || play.finished()) return;
        if (pl.gruzclient.cosmetics.EmoteAnim.handled(entity.getUUID(), play)) return; // gra biblioteka animacji (zginanie)
        play.emote().apply((HumanoidModel<?>) (Object) this, play);
    }
}
//#endif
