package pl.gruzclient.mixin;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.render.Ui;
import pl.gruzclient.screen.HudEditorScreen;
import pl.gruzclient.screen.UiButton;

/** Przycisk "Gruz Client Mods" w menu pauzy (ESC). */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void gruz$addModsButton(CallbackInfo ci) {
        Screen self = this;
        this.addDrawableChild(new UiButton(8, 8, 120, 22, Ui.Icon.GRID, Lang.t("menu.mods"),
                b -> this.client.setScreen(new HudEditorScreen(self))).kind(UiButton.Kind.ACCENT).big());
    }
}
