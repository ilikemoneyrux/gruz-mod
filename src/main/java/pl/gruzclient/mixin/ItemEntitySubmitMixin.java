//#only MC >= 1.21.9
package pl.gruzclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;
//#if MC >= 26.1
import net.minecraft.client.renderer.state.level.CameraRenderState;
//#else
import net.minecraft.client.renderer.state.CameraRenderState;
//#endif

/**
 * Item physics od 1.21.9: przedmioty na ziemi idą przez render state (ItemEntityRenderState) i submit,
 * więc zamiast unoszenia i kręcenia kładziemy je płasko (bloki stoją na ziemi), każdy obrócony inaczej.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntitySubmitMixin {
    @Unique private final RandomSource gruz$random = RandomSource.create();

    //#if MC >= 26.1
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    //#else
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    //#endif
    private void gruz$physics(ItemEntityRenderState s, PoseStack m, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (!Modules.ITEM_PHYSICS.isEnabled() || s.item.isEmpty()) return;
        var box = s.item.getModelBoundingBox();
        float yaw = s.bobOffset * 57.2958f; // bobOffset jest losowy dla każdego przedmiotu – stały obrót
        m.pushPose();
        if (s.item.usesBlockLight()) {
            // bloki stoją na ziemi
            m.translate(0f, (float) -box.minY, 0f);
            m.mulPose(Axis.YP.rotationDegrees(yaw));
        } else {
            // płaskie przedmioty leżą na ziemi
            m.translate(0f, 0.02f, 0f);
            m.mulPose(Axis.YP.rotationDegrees(yaw));
            m.mulPose(Axis.XP.rotationDegrees(90f));
        }
        ItemEntityRenderer.submitMultipleFromCount(m, collector, s.lightCoords, s, gruz$random, box);
        m.popPose();
        ci.cancel();
    }
}
