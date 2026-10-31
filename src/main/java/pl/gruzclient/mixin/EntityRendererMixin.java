package pl.gruzclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
//#if MC < 26.2
import net.minecraft.client.renderer.MultiBufferSource;
//#endif
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
    //#if MC >= 1.21.2
    // od 1.21.2 renderowanie dostaje gotowy "render state" bez encji, więc podmieniamy tekst tam, gdzie powstaje
    @org.spongepowered.asm.mixin.injection.Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true, require = 0)
    private void gruz$recolor(T entity, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Component> cir) {
        Component text = cir.getReturnValue();
        if (text != null) cir.setReturnValue(Modules.NAMETAGS.label(entity, text));
    }
    //#else
    @ModifyVariable(method = "renderNameTag", at = @At("HEAD"), argsOnly = true, require = 0)
    private Component gruz$recolor(Component text, T entity, Component original, PoseStack matrices, MultiBufferSource vertexConsumers,
                              int light
                              //#if MC >= 1.20.5
                              , float tickDelta
                              //#endif
                              ) {
        return Modules.NAMETAGS.label(entity, text);
    }
    //#endif
}
