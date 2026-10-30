package pl.gruzclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.module.Modules;

/** Tab: logo Gruz, statystyki Hypixela i ping w ms */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, require = 0)
    private void gruz$name(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(Modules.TAB.name(entry, cir.getReturnValue()));
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$ping(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (Modules.TAB.drawPing(context, width, x, y, entry)) ci.cancel();
    }
}
