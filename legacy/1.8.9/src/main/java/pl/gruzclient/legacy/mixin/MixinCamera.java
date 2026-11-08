package pl.gruzclient.legacy.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.legacy.Hud;
import pl.gruzclient.legacy.Mods;

/** Zoom (FOV) i freelook (kąt kamery zamiast kąta postaci w transformCamera) */
@Mixin(GameRenderer.class)
public abstract class MixinCamera {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void gruz$zoom(float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (Mods.zooming()) cir.setReturnValue(cir.getReturnValue() / 4f);
    }

    @Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;yaw:F", opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float gruz$yaw(Entity e) { return Mods.freelookActive ? Mods.lookYaw : e.yaw; }

    @Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevYaw:F", opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float gruz$prevYaw(Entity e) { return Mods.freelookActive ? Mods.lookYaw : e.prevYaw; }

    @Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;pitch:F", opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float gruz$pitch(Entity e) { return Mods.freelookActive ? Mods.lookPitch : e.pitch; }

    @Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevPitch:F", opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float gruz$prevPitch(Entity e) { return Mods.freelookActive ? Mods.lookPitch : e.prevPitch; }
}
