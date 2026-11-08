//#only MC >= 1.21.9
//#swap MC >= 26.2 | "renderHitOutline" => "submitHitOutline"
package pl.gruzclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;
//#if MC >= 26.1
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
//#else
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
//#endif

/**
 * Block Overlay od 1.21.9: gra rysuje obrys z gotowego stanu (BlockOutlineRenderState) i sama umie grube linie,
 * więc podmieniamy kolor i grubość, a półprzezroczyste wypełnienie dokładamy osobno.
 */
@Mixin(LevelRenderer.class)
public abstract class BlockOutlineMixin {
    @ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int gruz$color(int color) {
        return Modules.BLOCK_OVERLAY.outlineArgb(color);
    }

    @ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float gruz$width(float width) {
        return Modules.BLOCK_OVERLAY.lineWidth(width);
    }

    //#if MC >= 26.2
    /** 26.2+: obrys idzie przez SubmitNodeCollector – wypełnienie zgłaszamy jako własną geometrię */
    @Inject(method = "submitBlockOutline", at = @At("TAIL"), require = 0)
    private void gruz$fill(PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector,
                           net.minecraft.client.renderer.state.level.LevelRenderState state, CallbackInfo ci) {
        var m = Modules.BLOCK_OVERLAY;
        BlockOutlineRenderState o = state.blockOutlineRenderState;
        if (!m.wantsFill() || o == null || o.shape().isEmpty() || state.cameraRenderState == null) return;
        var cam = state.cameraRenderState.pos;
        double ox = o.pos().getX() - cam.x, oy = o.pos().getY() - cam.y, oz = o.pos().getZ() - cam.z;
        collector.submitCustomGeometry(pose, net.minecraft.client.renderer.rendertype.RenderTypes.debugQuads(),
                (p, vc) -> m.fillShape(p, vc, o.shape(), ox, oy, oz));
    }
    //#else
    /** Do 26.1: po narysowaniu linii dokładamy ścianki do wspólnego bufora (gra opróżnia go zaraz potem – endLastBatch) */
    @Inject(method = "renderHitOutline", at = @At("TAIL"), require = 0)
    private void gruz$fill(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer lines, double camX, double camY, double camZ,
                           BlockOutlineRenderState o, int color, float width, CallbackInfo ci) {
        var m = Modules.BLOCK_OVERLAY;
        if (!m.wantsFill() || o == null || o.shape().isEmpty()) return;
        var vc = net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource()
                .getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.debugQuads());
        m.fillShape(pose.last(), vc, o.shape(), o.pos().getX() - camX, o.pos().getY() - camY, o.pos().getZ() - camZ);
    }
    //#endif
}
