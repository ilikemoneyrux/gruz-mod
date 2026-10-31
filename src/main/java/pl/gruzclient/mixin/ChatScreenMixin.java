//#swap MC >= 26.1 | "render" => "extractRenderState"
package pl.gruzclient.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gruzclient.chat.GruzChat;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.impl.BetterChatModule;
import pl.gruzclient.render.GruzDraw;

/** Czat: Ctrl+F otwiera wyszukiwanie, prawy przycisk myszy kopiuje wiadomość. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Unique private EditBox gruz$search;
    @Unique private String gruz$toast = "";
    @Unique private long gruz$toastAt = 0;

    protected ChatScreenMixin(Component title) { super(title); }

    @Unique
    /** Dodatki czatu (ChatHudMixin) są tylko od 1.20.5 – na starszych wersjach zwracamy atrapę */
    private GruzChat gruz$chat() {
        Object chat = this.minecraft.gui.getChat();
        return chat instanceof GruzChat g ? g : GruzChat.NONE;
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void gruz$init(CallbackInfo ci) {
        if (!Modules.BETTER_CHAT.isEnabled()) return;
        gruz$search = new EditBox(this.font, 4, this.height - 32, 180, 12, Component.literal(Lang.t("chat.search")));
        gruz$search.setHint(Component.literal(Lang.t("chat.search")));
        gruz$search.setMaxLength(64);
        gruz$search.setVisible(!BetterChatModule.filter.isEmpty());
        gruz$search.setValue(BetterChatModule.filter);
        gruz$search.setResponder(s -> {
            BetterChatModule.filter = s;
            gruz$chat().gruz$rebuild();
        });
        this.addRenderableWidget(gruz$search);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    //#if MC >= 1.21.9
    private void gruz$keys(net.minecraft.client.input.KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = event.key();
    //#else
    private void gruz$keys(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        if (gruz$search == null) return;
        if (keyCode == GLFW.GLFW_KEY_F && pl.gruzclient.compat.GruzScreen.ctrl()) {
            boolean show = !gruz$search.isVisible();
            gruz$search.setVisible(show);
            if (show) { this.setFocused(gruz$search); gruz$search.setFocused(true); }
            else { gruz$search.setValue(""); }
            cir.setReturnValue(true);
            return;
        }
        // Enter/strzałki w polu wyszukiwania nie wysyłają wiadomości
        if (gruz$search.isVisible() && gruz$search.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
                this.setFocused(null);
                cir.setReturnValue(true);
                return;
            }
            //#if MC >= 1.21.9
            cir.setReturnValue(gruz$search.keyPressed(event));
            //#else
            cir.setReturnValue(gruz$search.keyPressed(keyCode, scanCode, modifiers));
            //#endif
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    //#if MC >= 1.21.9
    private void gruz$copy(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    //#else
    private void gruz$copy(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        if (button != GLFW.GLFW_MOUSE_BUTTON_RIGHT || !Modules.BETTER_CHAT.copying()) return;
        String msg = gruz$chat().gruz$messageAt(mouseX, mouseY);
        if (msg == null || msg.isEmpty()) return;
        this.minecraft.keyboardHandler.setClipboard(msg);
        gruz$toast = Lang.t("chat.copied");
        gruz$toastAt = System.currentTimeMillis();
        cir.setReturnValue(true);
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void gruz$render(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (gruz$search != null && gruz$search.isVisible()) {
            context.drawString(this.font, Lang.t("chat.searchHint"), 190, this.height - 30, GruzDraw.argb(1f, GruzDraw.MUTE), true);
        }
        if (!gruz$toast.isEmpty() && System.currentTimeMillis() - gruz$toastAt < 1500) {
            context.drawString(this.font, gruz$toast, 4, this.height - 44, GruzDraw.argb(1f, GruzDraw.NEON_HOT), true);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"), require = 0)
    private void gruz$removed(CallbackInfo ci) {
        if (!BetterChatModule.filter.isEmpty()) {
            BetterChatModule.filter = "";
            gruz$chat().gruz$rebuild();
        }
    }
}
