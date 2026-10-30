package pl.gruzclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;

import static pl.gruzclient.render.GruzDraw.*;

/** Małe okienko do wpisania tekstu ustawienia (np. hasło OBS). Enter zapisuje, Esc wraca. */
public class TextInputScreen extends Screen {
    private final Screen parent;
    private final Setting.Text setting;
    private TextFieldWidget field;

    public TextInputScreen(Screen parent, Setting.Text setting) {
        super(Text.literal(setting.label()));
        this.parent = parent;
        this.setting = setting;
    }

    @Override
    protected void init() {
        int w = Math.min(260, width - 40), x = (width - w) / 2, y = height / 2 - 10;
        field = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(setting.label()));
        field.setMaxLength(200);
        field.setText(setting.value);
        if (setting.secret) field.setRenderTextProvider((s, i) -> net.minecraft.text.OrderedText.styledForwardsVisitedString("•".repeat(s.length()), net.minecraft.text.Style.EMPTY));
        addDrawableChild(field);
        setInitialFocus(field);
        addDrawableChild(new GruzButton(x, y + 30, w / 2 - 3, 20, Text.literal(Lang.t("mods.save")), b -> save()).primary());
        addDrawableChild(new GruzButton(x + w / 2 + 3, y + 30, w / 2 - 3, 20, Text.literal(Lang.t("mods.back")), b -> close()));
    }

    private void save() {
        setting.value = field.getText().trim();
        Modules.save();
        close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { save(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, client.world != null ? 0xA0000000 : 0xFF07050C);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        String title = setting.label();
        c.drawText(textRenderer, title, (width - textRenderer.getWidth(title)) / 2, height / 2 - 28, argb(1f, NEON_HOT), true);
    }
}
