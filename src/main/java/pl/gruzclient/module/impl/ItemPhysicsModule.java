package pl.gruzclient.module.impl;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import pl.gruzclient.module.Module;

/** Item physics: przedmioty leżą płasko na ziemi zamiast unosić się i obracać. */
public class ItemPhysicsModule extends Module {
    public ItemPhysicsModule() { super("itemphysics", Category.VISUAL, false); }

    /** Rysuje przedmiot leżący na ziemi. Zwraca false, gdy ma zostać zwykłe renderowanie. */
    public boolean render(ItemEntity entity, float tickDelta, MatrixStack m, VertexConsumerProvider buffers, int light, ItemRenderer itemRenderer) {
        if (!isEnabled()) return false;
        ItemStack stack = entity.getStack();
        if (stack.isEmpty()) return false;
        int seed = stack.isEmpty() ? 187 : net.minecraft.item.Item.getRawId(stack.getItem()) + stack.getDamage();
        BakedModel model = itemRenderer.getModel(stack, entity.getWorld(), null, entity.getId());
        boolean block = model.hasDepth();
        float yaw = (entity.getUuid().getLeastSignificantBits() & 0xFFFF) / 65535f * 360f;
        boolean ground = entity.isOnGround();
        float spin = ground ? 0f : (entity.getItemAge() + tickDelta) * 12f;

        m.push();
        if (block) {
            // bloki stoją na ziemi, lekko obrócone
            m.translate(0, 0.125f, 0);
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + spin));
            m.scale(0.9f, 0.9f, 0.9f);
        } else {
            // płaskie przedmioty kładziemy na ziemi
            m.translate(0, 0.02f, 0);
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f + (ground ? 0f : spin)));
            m.scale(0.85f, 0.85f, 0.85f);
        }
        int copies = stack.getCount() > 48 ? 4 : stack.getCount() > 32 ? 3 : stack.getCount() > 1 ? 2 : 1;
        for (int i = 0; i < copies; i++) {
            m.push();
            if (i > 0) {
                float ox = ((seed * (i + 3) * 31) % 100) / 100f * 0.12f - 0.06f;
                float oy = ((seed * (i + 7) * 17) % 100) / 100f * 0.12f - 0.06f;
                if (block) m.translate(ox, 0.06f * i, oy);
                else m.translate(ox, oy, -0.03f * i);
            }
            itemRenderer.renderItem(stack, ModelTransformationMode.GROUND, false, m, buffers, light, OverlayTexture.DEFAULT_UV, model);
            m.pop();
        }
        m.pop();
        return true;
    }
}
