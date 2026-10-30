package pl.gruzclient.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Tab: logo Gruz, statystyki Hypixela i ping w ms */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, require = 0)
    private void gruz$name(PlayerInfo entry, CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(Modules.TAB.name(entry, cir.getReturnValue()));
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$ping(GuiGraphics context, int width, int x, int y, PlayerInfo entry, CallbackInfo ci) {
        if (Modules.TAB.drawPing(context, width, x, y, entry)) ci.cancel();
    }
}
