package pl.gruzclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
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
    @Unique private TextFieldWidget gruz$search;
    @Unique private String gruz$toast = "";
    @Unique private long gruz$toastAt = 0;

    protected ChatScreenMixin(Text title) { super(title); }

    @Unique
    private GruzChat gruz$chat() { return (GruzChat) this.client.inGameHud.getChatHud(); }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void gruz$init(CallbackInfo ci) {
        if (!Modules.BETTER_CHAT.isEnabled()) return;
        gruz$search = new TextFieldWidget(this.textRenderer, 4, this.height - 32, 180, 12, Text.literal(Lang.t("chat.search")));
        gruz$search.setPlaceholder(Text.literal(Lang.t("chat.search")));
        gruz$search.setMaxLength(64);
        gruz$search.setVisible(!BetterChatModule.filter.isEmpty());
        gruz$search.setText(BetterChatModule.filter);
        gruz$search.setChangedListener(s -> {
            BetterChatModule.filter = s;
            gruz$chat().gruz$rebuild();
        });
        this.addDrawableChild(gruz$search);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$keys(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (gruz$search == null) return;
        if (keyCode == GLFW.GLFW_KEY_F && Screen.hasControlDown()) {
            boolean show = !gruz$search.isVisible();
            gruz$search.setVisible(show);
            if (show) { this.setFocused(gruz$search); gruz$search.setFocused(true); }
            else { gruz$search.setText(""); }
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
            cir.setReturnValue(gruz$search.keyPressed(keyCode, scanCode, modifiers));
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$copy(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_RIGHT || !Modules.BETTER_CHAT.copying()) return;
        String msg = gruz$chat().gruz$messageAt(mouseX, mouseY);
        if (msg == null || msg.isEmpty()) return;
        this.client.keyboard.setClipboard(msg);
        gruz$toast = Lang.t("chat.copied");
        gruz$toastAt = System.currentTimeMillis();
        cir.setReturnValue(true);
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void gruz$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (gruz$search != null && gruz$search.isVisible()) {
            context.drawText(this.textRenderer, Lang.t("chat.searchHint"), 190, this.height - 30, GruzDraw.argb(1f, GruzDraw.MUTE), true);
        }
        if (!gruz$toast.isEmpty() && System.currentTimeMillis() - gruz$toastAt < 1500) {
            context.drawText(this.textRenderer, gruz$toast, 4, this.height - 44, GruzDraw.argb(1f, GruzDraw.NEON_HOT), true);
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
