package pl.gruzclient.screen;

import com.mojang.blaze3d.platform.InputConstants;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;

import static pl.gruzclient.render.GruzDraw.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Małe okienko do wpisania tekstu ustawienia (np. hasło OBS). Enter zapisuje, Esc wraca. */
public class TextInputScreen extends pl.gruzclient.compat.GruzScreen {
    private final Screen parent;
    private final Setting.Text setting;
    private EditBox field;

    public TextInputScreen(Screen parent, Setting.Text setting) {
        super(Component.literal(setting.label()));
        this.parent = parent;
        this.setting = setting;
    }

    @Override
    protected void init() {
        int w = Math.min(260, width - 40), x = (width - w) / 2, y = height / 2 - 10;
        field = new EditBox(font, x, y, w, 20, Component.literal(setting.label()));
        field.setMaxLength(200);
        field.setValue(setting.value);
        if (setting.secret) field.
                //#if MC >= 1.21.9
                addFormatter(
                //#else
                setFormatter(
                //#endif
                (s, i) -> net.minecraft.util.FormattedCharSequence.forward("•".repeat(s.length()), net.minecraft.network.chat.Style.EMPTY));
        addRenderableWidget(field);
        setInitialFocus(field);
        addRenderableWidget(new GruzButton(x, y + 30, w / 2 - 3, 20, Component.literal(Lang.t("mods.save")), b -> save()).primary());
        addRenderableWidget(new GruzButton(x + w / 2 + 3, y + 30, w / 2 - 3, 20, Component.literal(Lang.t("mods.back")), b -> onClose()));
    }

    private void save() {
        setting.value = field.getValue().trim();
        Modules.save();
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) { save(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public void renderBackground(GuiGraphics c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, minecraft.level != null ? 0xA0000000 : 0xFF07050C);
    }

    @Override
    public void render(GuiGraphics c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        String title = setting.label();
        c.drawString(font, title, (width - font.width(title)) / 2, height / 2 - 28, argb(1f, NEON_HOT), true);
    }
}
