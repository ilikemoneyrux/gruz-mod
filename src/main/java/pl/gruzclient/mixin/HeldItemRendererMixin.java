package pl.gruzclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

@Mixin(ItemInHandRenderer.class)
public abstract class HeldItemRendererMixin {
    /** Niżej trzymana tarcza (gdy jej nie używasz) */
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$shieldDown(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress,
                                 ItemStack item, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers,
                                 int light, CallbackInfo ci) {
        // blok mieczem: tarczy nie widać, a miecz ustawia się jak w 1.8
        if (item.is(Items.SHIELD) && Modules.OLD_ANIM.swordBlocking(player)) { ci.cancel(); return; }
        matrices.pushPose();
        if (item.is(Items.SHIELD) && !(player.isUsingItem() && player.getUsedItemHand() == hand)) {
            matrices.translate(0f, -Modules.LOW_FIRE.shieldOffset(), 0f);
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"), require = 0)
    private void gruz$shieldRestore(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress,
                                    ItemStack item, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers,
                                    int light, CallbackInfo ci) {
        matrices.popPose();
    }

    /** Poza bloku mieczem z 1.8 (doBlockTransformations), tuż przed narysowaniem miecza */
    @Inject(method = "renderArmWithItem", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void gruz$swordBlock(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress,
                                 ItemStack item, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers,
                                 int light, CallbackInfo ci) {
        if (!(item.getItem() instanceof net.minecraft.world.item.SwordItem) || !Modules.OLD_ANIM.swordBlocking(player)) return;
        boolean right = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
        float a = right ? 1f : -1f;
        matrices.translate(a * -0.14142136f, 0.08f, 0.14142136f);
        matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-102.25f));
        matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(a * 13.365f));
        matrices.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(a * 78.05f));
    }

    /** Animacje 1.7: ręka nie opada po ciosie (ignorujemy cooldown ataku) */
    @Redirect(method = "tick", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    private float gruz$noCooldownDip(LocalPlayer player, float baseTime) {
        return Modules.OLD_ANIM.noCooldown() ? 1.0f : player.getAttackStrengthScale(baseTime);
    }
}
