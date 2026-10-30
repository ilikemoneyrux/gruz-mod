package pl.gruzclient.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.network.GruzNetwork;

/**
 * Komendy Gruz w czacie (nie trafiają na serwer):
 *  .msg Nick tekst   – wiadomość do znajomego z Gruz
 *  .r tekst          – odpowiedź na ostatnią wiadomość
 *  .warp             – lider party ściąga wszystkich na swój serwer
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$commands(String content, CallbackInfo ci) {
        if (!content.startsWith(".")) return;
        String[] parts = content.substring(1).split(" ", 3);
        String cmd = parts[0].toLowerCase();
        switch (cmd) {
            case "msg", "m", "w" -> {
                if (parts.length < 3) help();
                else GruzNetwork.sendDm(parts[1], parts[2]);
            }
            case "r" -> {
                String text = content.substring(content.indexOf(' ') + 1).trim();
                if (text.isEmpty() || !content.contains(" ")) help();
                else GruzNetwork.reply(text);
            }
            case "warp" -> GruzNetwork.partyWarp();
            case "gruz", "help" -> help();
            default -> { return; } // inne kropki (np. ".") idą normalnie na serwer
        }
        Minecraft.getInstance().gui.getChat().addRecentChat(content);
        ci.cancel();
    }

    private static void help() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (String line : Lang.t("net.help").split("\\|")) mc.player.displayClientMessage(Component.literal(line).withStyle(ChatFormatting.GRAY), false);
    }
}
