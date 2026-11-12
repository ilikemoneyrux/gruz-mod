package pl.gruzclient.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Modules;
import pl.gruzclient.render.Ui;
import pl.gruzclient.screen.UiButton;

/** Przycisk "Menu Gruz" w klasycznym menu głównym (widać je tylko po przełączeniu na klasyczne). */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void gruz$addGruzMenuButton(CallbackInfo ci) {
        this.addRenderableWidget(new UiButton(8, 8, 120, 22, Ui.Icon.LAYOUT, Lang.t("menu.gruzMenu"),
                b -> Modules.MENU.setClassic(false)).kind(UiButton.Kind.ACCENT).big());
    }
}
