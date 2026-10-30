package pl.gruzclient.mixin;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.render.Ui;
import pl.gruzclient.screen.HudEditorScreen;
import pl.gruzclient.screen.UiButton;

/** Przycisk "Gruz Client Mods" w menu pauzy (ESC). */
@Mixin(PauseScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void gruz$addModsButton(CallbackInfo ci) {
        Screen self = this;
        this.addRenderableWidget(new UiButton(8, 8, 120, 22, Ui.Icon.GRID, Lang.t("menu.mods"),
                b -> this.minecraft.setScreen(new HudEditorScreen(self))).kind(UiButton.Kind.ACCENT).big());
    }
}
