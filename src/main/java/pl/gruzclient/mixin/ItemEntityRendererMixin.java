package pl.gruzclient.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

/** Item physics: własne rysowanie przedmiotów leżących na ziemi. */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Shadow @Final private ItemRenderer itemRenderer;

    @Inject(method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$physics(ItemEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider buffers, int light, CallbackInfo ci) {
        if (Modules.ITEM_PHYSICS.render(entity, tickDelta, matrices, buffers, light, this.itemRenderer)) ci.cancel();
    }
}
