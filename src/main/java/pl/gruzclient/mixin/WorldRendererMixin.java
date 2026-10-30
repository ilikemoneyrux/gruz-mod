package pl.gruzclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.impl.BlockOverlayModule;

/** Block Overlay: kolor obrysu i półprzezroczyste wypełnienie bloku, na który patrzysz. */
@Mixin(LevelRenderer.class)
public abstract class WorldRendererMixin {
    @ModifyArgs(method = "renderHitOutline", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderShape(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/shapes/VoxelShape;DDDFFFF)V"))
    private void gruz$outlineColor(Args args) {
        BlockOverlayModule m = Modules.BLOCK_OVERLAY;
        if (!m.isEnabled()) return;
        float[] c = m.rgba(m.thick() ? 0f : m.outlineAlpha.value / 100f);
        args.set(6, c[0]);
        args.set(7, c[1]);
        args.set(8, c[2]);
        args.set(9, c[3]);
    }

    @Inject(method = "renderHitOutline", at = @At("TAIL"), require = 0)
    private void gruz$fill(PoseStack matrices, VertexConsumer vertexConsumer, Entity entity, double cameraX, double cameraY, double cameraZ,
                           BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockOverlayModule m = Modules.BLOCK_OVERLAY;
        if (!m.isEnabled() || (!m.fill.value && !m.thick())) return;
        Minecraft mc = Minecraft.getInstance();
        VoxelShape shape = state.getShape(mc.level, pos);
        if (shape.isEmpty()) return;
        VertexConsumer fill = mc.renderBuffers().bufferSource().getBuffer(RenderType.debugFilledBox());
        double ox = pos.getX() - cameraX, oy = pos.getY() - cameraY, oz = pos.getZ() - cameraZ;
        if (m.thick()) {
            // grubość w blokach rośnie z odległością, żeby linia wyglądała tak samo z bliska i z daleka
            double dist = Math.sqrt((pos.getX() + 0.5 - cameraX) * (pos.getX() + 0.5 - cameraX)
                    + (pos.getY() + 0.5 - cameraY) * (pos.getY() + 0.5 - cameraY) + (pos.getZ() + 0.5 - cameraZ) * (pos.getZ() + 0.5 - cameraZ));
            double t = m.thickness.value * 0.0016 * Math.max(1.5, dist);
            float[] lc = m.rgba(m.outlineAlpha.value / 100f);
            shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> LevelRenderer.addChainedFilledBoxVertices(matrices, fill,
                    Math.min(x1, x2) + ox - t, Math.min(y1, y2) + oy - t, Math.min(z1, z2) + oz - t,
                    Math.max(x1, x2) + ox + t, Math.max(y1, y2) + oy + t, Math.max(z1, z2) + oz + t,
                    lc[0], lc[1], lc[2], lc[3]));
        }
        if (!m.fill.value) return;
        float[] c = m.rgba(m.fillAlpha.value / 100f * m.pulseFactor());
        for (AABB b : shape.toAabbs()) {
            LevelRenderer.addChainedFilledBoxVertices(matrices, fill,
                    b.minX + ox - 0.002, b.minY + oy - 0.002, b.minZ + oz - 0.002,
                    b.maxX + ox + 0.002, b.maxY + oy + 0.002, b.maxZ + oz + 0.002,
                    c[0], c[1], c[2], c[3]);
        }
    }
}
