package pl.gruzclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pl.gruzclient.module.Modules;

/** Własny kolor nicków nad głowami graczy. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @ModifyVariable(method = "renderNameTag", at = @At("HEAD"), argsOnly = true, require = 0)
    private Component gruz$recolor(Component text, T entity, Component original, PoseStack matrices, MultiBufferSource vertexConsumers,
                              int light, float tickDelta) {
        return Modules.NAMETAGS.label(entity, text);
    }
}
