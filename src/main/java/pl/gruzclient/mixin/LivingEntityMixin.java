package pl.gruzclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Animacje 1.7: szybkość machania ręką (tylko Twoja postać). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
    private void gruz$swing(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this == Minecraft.getInstance().player) {
            cir.setReturnValue(Modules.OLD_ANIM.swingDuration(cir.getReturnValue()));
        }
    }
}
