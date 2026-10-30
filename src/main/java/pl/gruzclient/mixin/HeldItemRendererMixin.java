package pl.gruzclient.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    /** Niżej trzymana tarcza (gdy jej nie używasz) */
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$shieldDown(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress,
                                 ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                 int light, CallbackInfo ci) {
        // blok mieczem: tarczy nie widać, a miecz ustawia się jak w 1.8
        if (item.isOf(Items.SHIELD) && Modules.OLD_ANIM.swordBlocking(player)) { ci.cancel(); return; }
        matrices.push();
        if (item.isOf(Items.SHIELD) && !(player.isUsingItem() && player.getActiveHand() == hand)) {
            matrices.translate(0f, -Modules.LOW_FIRE.shieldOffset(), 0f);
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"), require = 0)
    private void gruz$shieldRestore(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress,
                                    ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                    int light, CallbackInfo ci) {
        matrices.pop();
    }

    /** Poza bloku mieczem z 1.8 (doBlockTransformations), tuż przed narysowaniem miecza */
    @Inject(method = "renderFirstPersonItem", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void gruz$swordBlock(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress,
                                 ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                 int light, CallbackInfo ci) {
        if (!(item.getItem() instanceof net.minecraft.item.SwordItem) || !Modules.OLD_ANIM.swordBlocking(player)) return;
        boolean right = (hand == Hand.MAIN_HAND) == (player.getMainArm() == net.minecraft.util.Arm.RIGHT);
        float a = right ? 1f : -1f;
        matrices.translate(a * -0.14142136f, 0.08f, 0.14142136f);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-102.25f));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(a * 13.365f));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(a * 78.05f));
    }

    /** Animacje 1.7: ręka nie opada po ciosie (ignorujemy cooldown ataku) */
    @Redirect(method = "updateHeldItems", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getAttackCooldownProgress(F)F"))
    private float gruz$noCooldownDip(ClientPlayerEntity player, float baseTime) {
        return Modules.OLD_ANIM.noCooldown() ? 1.0f : player.getAttackCooldownProgress(baseTime);
    }
}
