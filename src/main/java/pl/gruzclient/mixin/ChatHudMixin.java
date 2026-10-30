package pl.gruzclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.chat.GruzChat;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.impl.BetterChatModule;

import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

/**
 * Lepszy czat: zwijanie powtórzeń, godziny, dłuższa historia, filtr wyszukiwania i kopiowanie.
 * Przy okazji przekazuje wiadomości do Auto GG.
 */
@Mixin(ChatComponent.class)
public abstract class ChatHudMixin implements GruzChat {
    @Shadow @Final private List<GuiMessage> messages;
    @Shadow @Final private List<GuiMessage.Line> visibleMessages;
    @Shadow private int scrolledLines;

    @Shadow private void addVisibleMessage(GuiMessage message) { throw new AssertionError(); }
    @Shadow private double toChatLineX(double x) { throw new AssertionError(); }
    @Shadow private double toChatLineY(double y) { throw new AssertionError(); }
    @Shadow private int getMessageLineIndex(double chatLineX, double chatLineY) { throw new AssertionError(); }

    @Unique private String gruz$lastPlain = null;
    @Unique private int gruz$count = 1;

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private Component gruz$onMessage(Component message, Component original, MessageSignature signature, GuiMessageTag indicator) {
        String plain = message.getString();
        Modules.AUTO_GG.onChat(plain);
        BetterChatModule chat = Modules.BETTER_CHAT;
        if (chat.stacking() && plain.equals(gruz$lastPlain) && !this.messages.isEmpty()) {
            gruz$count++;
            gruz$removeNewest();
            return chat.decorate(BetterChatModule.withCount(message, gruz$count));
        }
        gruz$lastPlain = plain;
        gruz$count = 1;
        return chat.decorate(message);
    }

    /** Usuwa najnowszą wiadomość (jej linie są na początku listy widocznych) */
    @Unique
    private void gruz$removeNewest() {
        this.messages.remove(0);
        if (this.visibleMessages.isEmpty()) return;
        this.visibleMessages.remove(0); // dolna linia wiadomości (endOfEntry)
        while (!this.visibleMessages.isEmpty() && !this.visibleMessages.get(0).endOfEntry()) this.visibleMessages.remove(0);
    }

    /** Dłuższa historia czatu (1000 zamiast 100 wiadomości) */
    @ModifyConstant(method = {"addVisibleMessage", "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"},
            constant = @Constant(intValue = 100), require = 0)
    private int gruz$history(int original) {
        return Modules.BETTER_CHAT.historyLimit();
    }

    /** Filtr wyszukiwania: pomijamy linie niepasujące do frazy */
    @Inject(method = "addVisibleMessage", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$filter(GuiMessage message, CallbackInfo ci) {
        if (!BetterChatModule.matches(message.content())) ci.cancel();
    }

    @Override
    public void gruz$rebuild() {
        this.visibleMessages.clear();
        this.scrolledLines = 0;
        for (int i = this.messages.size() - 1; i >= 0; i--) this.addVisibleMessage(this.messages.get(i));
    }

    @Override
    public String gruz$messageAt(double mouseX, double mouseY) {
        int idx = this.getMessageLineIndex(this.toChatLineX(mouseX), this.toChatLineY(mouseY));
        if (idx < 0 || idx >= this.visibleMessages.size()) return null;
        // linie jednej wiadomości: od dolnej (endOfEntry) w górę do następnej endOfEntry
        int bottom = idx;
        while (bottom > 0 && !this.visibleMessages.get(bottom).endOfEntry()) bottom--;
        int top = idx;
        while (top + 1 < this.visibleMessages.size() && !this.visibleMessages.get(top + 1).endOfEntry()) top++;
        StringBuilder sb = new StringBuilder();
        for (int i = top; i >= bottom; i--) sb.append(GruzChat.plain(this.visibleMessages.get(i).content()));
        return sb.toString();
    }
}
