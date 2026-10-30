package pl.gruzclient.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pl.gruzclient.module.Modules;

/** Własny kolor nicków nad głowami graczy. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text gruz$recolor(Text text, T entity, Text original, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                              int light, float tickDelta) {
        return Modules.NAMETAGS.label(entity, text);
    }
}
