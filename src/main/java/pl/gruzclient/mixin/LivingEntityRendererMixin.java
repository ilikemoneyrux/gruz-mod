package pl.gruzclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Pokazuj własny nick w widoku z trzeciej osoby (F5). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {
    //#if MC >= 1.21.2
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$ownLabel(T entity, double distance, CallbackInfoReturnable<Boolean> cir) {
    //#else
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$ownLabel(T entity, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        Minecraft mc = Minecraft.getInstance();
        if (Modules.NAMETAGS.isEnabled() && Modules.NAMETAGS.showOwn.value && entity == mc.player
                && !mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui) {
            cir.setReturnValue(true);
        }
    }
}
