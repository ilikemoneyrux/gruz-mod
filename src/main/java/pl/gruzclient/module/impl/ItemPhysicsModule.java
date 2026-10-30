package pl.gruzclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import pl.gruzclient.module.Module;

/** Item physics: przedmioty leżą płasko na ziemi zamiast unosić się i obracać. */
public class ItemPhysicsModule extends Module {
    public ItemPhysicsModule() { super("itemphysics", Category.VISUAL, false); }

    /** Rysuje przedmiot leżący na ziemi. Zwraca false, gdy ma zostać zwykłe renderowanie. */
    public boolean render(ItemEntity entity, float tickDelta, PoseStack m, MultiBufferSource buffers, int light, ItemRenderer itemRenderer) {
        if (!isEnabled()) return false;
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) return false;
        int seed = stack.isEmpty() ? 187 : net.minecraft.world.item.Item.getId(stack.getItem()) + stack.getDamageValue();
        BakedModel model = itemRenderer.getModel(stack, entity.level(), null, entity.getId());
        boolean block = model.isGui3d();
        float yaw = (entity.getUUID().getLeastSignificantBits() & 0xFFFF) / 65535f * 360f;
        boolean ground = entity.onGround();
        float spin = ground ? 0f : (entity.getAge() + tickDelta) * 12f;

        m.pushPose();
        if (block) {
            // bloki stoją na ziemi, lekko obrócone
            m.translate(0, 0.125f, 0);
            m.mulPose(Axis.YP.rotationDegrees(yaw + spin));
            m.scale(0.9f, 0.9f, 0.9f);
        } else {
            // płaskie przedmioty kładziemy na ziemi
            m.translate(0, 0.02f, 0);
            m.mulPose(Axis.YP.rotationDegrees(yaw));
            m.mulPose(Axis.XP.rotationDegrees(90f + (ground ? 0f : spin)));
            m.scale(0.85f, 0.85f, 0.85f);
        }
        int copies = stack.getCount() > 48 ? 4 : stack.getCount() > 32 ? 3 : stack.getCount() > 1 ? 2 : 1;
        for (int i = 0; i < copies; i++) {
            m.pushPose();
            if (i > 0) {
                float ox = ((seed * (i + 3) * 31) % 100) / 100f * 0.12f - 0.06f;
                float oy = ((seed * (i + 7) * 17) % 100) / 100f * 0.12f - 0.06f;
                if (block) m.translate(ox, 0.06f * i, oy);
                else m.translate(ox, oy, -0.03f * i);
            }
            itemRenderer.render(stack, ItemDisplayContext.GROUND, false, m, buffers, light, OverlayTexture.NO_OVERLAY, model);
            m.popPose();
        }
        m.popPose();
        return true;
    }
}
