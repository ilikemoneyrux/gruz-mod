package pl.gruzclient.legacy.mixin;

import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.legacy.GruzNet;

/** Komendy Gruz na czacie (.friends, .msg, .r, .party, .gruz) nie idą na serwer Minecrafta */
@Mixin(ClientPlayerEntity.class)
public abstract class MixinChatSend {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void gruz$command(String message, CallbackInfo ci) {
        if (message != null && message.startsWith(".") && GruzNet.command(message)) ci.cancel();
    }
}
