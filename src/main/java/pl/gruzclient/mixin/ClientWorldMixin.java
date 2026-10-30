package pl.gruzclient.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;
import pl.gruzclient.render.GruzDraw;

/** Kosmiczne niebo: ciemnofioletowy kolor nieba i zawsze widoczne gwiazdy. */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void gruz$skyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (!Modules.WORLD.cosmic()) return;
        int c = GruzDraw.mix(GruzDraw.NEON, 0x000000, 0.86f);
        cir.setReturnValue(new Vec3d(((c >> 16) & 255) / 255.0, ((c >> 8) & 255) / 255.0, (c & 255) / 255.0));
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true, require = 0)
    private void gruz$stars(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (Modules.WORLD.cosmic()) cir.setReturnValue(1.0f);
    }
}
