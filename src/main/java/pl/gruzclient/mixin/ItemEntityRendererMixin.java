//#only MC < 1.21.2
package pl.gruzclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.item.ItemEntity;
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

    @Inject(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$physics(ItemEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (Modules.ITEM_PHYSICS.render(entity, tickDelta, matrices, buffers, light, this.itemRenderer)) ci.cancel();
    }
}
