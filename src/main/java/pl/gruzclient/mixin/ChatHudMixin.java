//#only MC >= 1.20.5
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
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;

    @Shadow private void addMessageToDisplayQueue(GuiMessage message) { throw new AssertionError(); }
    //#if MC < 1.21.9
    @Shadow private double screenToChatX(double x) { throw new AssertionError(); }
    @Shadow private double screenToChatY(double y) { throw new AssertionError(); }
    @Shadow private int getMessageLineIndexAt(double chatLineX, double chatLineY) { throw new AssertionError(); }
    //#else
    // od 1.21.9 nie ma getMessageLineIndexAt – liczymy wiersz pod kursorem sami (jak gra rysuje czat: od dołu, 40 px nad krawędzią)
    @Shadow private double getScale() { throw new AssertionError(); }
    @Shadow private int getLineHeight() { throw new AssertionError(); }
    @Shadow private int getWidth() { throw new AssertionError(); }
    @Shadow public abstract int getLinesPerPage();
    //#endif

    @Unique private String gruz$lastPlain = null;
    @Unique private int gruz$count = 1;

    // od 26.1 addMessage ma dodatkowy parametr GuiMessageSource – inaczej łączenie powtórzeń i AutoGG po cichu nie działają
    //#if MC >= 26.1
    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
    //#else
    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
    //#endif
            at = @At("HEAD"), argsOnly = true, require = 0)
    private Component gruz$onMessage(Component message) {
        String plain = message.getString();
        Modules.AUTO_GG.onChat(plain);
        BetterChatModule chat = Modules.BETTER_CHAT;
        if (chat.stacking() && plain.equals(gruz$lastPlain) && !this.allMessages.isEmpty()) {
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
        this.allMessages.remove(0);
        if (this.trimmedMessages.isEmpty()) return;
        this.trimmedMessages.remove(0); // dolna linia wiadomości (endOfEntry)
        while (!this.trimmedMessages.isEmpty() && !this.trimmedMessages.get(0).endOfEntry()) this.trimmedMessages.remove(0);
    }

    /** Dłuższa historia czatu (1000 zamiast 100 wiadomości) */
    @ModifyConstant(method = {"addMessageToDisplayQueue", "addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V"},
            constant = @Constant(intValue = 100), require = 0)
    private int gruz$history(int original) {
        return Modules.BETTER_CHAT.historyLimit();
    }

    /** Filtr wyszukiwania: pomijamy linie niepasujące do frazy */
    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$filter(GuiMessage message, CallbackInfo ci) {
        if (!BetterChatModule.matches(message.content())) ci.cancel();
    }

    @Override
    public void gruz$rebuild() {
        this.trimmedMessages.clear();
        this.chatScrollbarPos = 0;
        for (int i = this.allMessages.size() - 1; i >= 0; i--) this.addMessageToDisplayQueue(this.allMessages.get(i));
    }

    @Override
    public String gruz$messageAt(double mouseX, double mouseY) {
        //#if MC >= 1.21.9
        double scale = this.getScale();
        double cx = mouseX / scale - 4.0;
        double cy = (net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight() - mouseY - 40.0) / (scale * this.getLineHeight());
        if (cx < -4.0 || cx > Math.floor(this.getWidth() / scale)) return null;
        if (cy < 0 || cy >= Math.min(this.getLinesPerPage(), this.trimmedMessages.size())) return null;
        int idx = (int) Math.floor(cy + this.chatScrollbarPos);
        //#else
        int idx = this.getMessageLineIndexAt(this.screenToChatX(mouseX), this.screenToChatY(mouseY));
        //#endif
        if (idx < 0 || idx >= this.trimmedMessages.size()) return null;
        // linie jednej wiadomości: od dolnej (endOfEntry) w górę do następnej endOfEntry
        int bottom = idx;
        while (bottom > 0 && !this.trimmedMessages.get(bottom).endOfEntry()) bottom--;
        int top = idx;
        while (top + 1 < this.trimmedMessages.size() && !this.trimmedMessages.get(top + 1).endOfEntry()) top++;
        StringBuilder sb = new StringBuilder();
        for (int i = top; i >= bottom; i--) sb.append(GruzChat.plain(this.trimmedMessages.get(i).content()));
        return sb.toString();
    }
}
