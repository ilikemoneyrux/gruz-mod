package pl.gruzclient.module.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import pl.gruzclient.module.HudModule;
import pl.gruzclient.module.Setting;

import java.util.HashMap;
import java.util.Map;

import static pl.gruzclient.render.GruzDraw.*;

/** Keystrokes: W A S D, przyciski myszy z CPS i spacja. Wciśnięcie płynnie podświetla klawisz. */
public class KeystrokesModule extends HudModule {
    public final Setting.Bool showMouse = add(new Setting.Bool("mouse", "setting.showMouse", true));
    public final Setting.Bool showSpace = add(new Setting.Bool("space", "setting.showSpace", true));

    private final Map<String, Float> glow = new HashMap<>();
    private long last = 0;
    private float dt = 0.016f;

    public KeystrokesModule() { super("keystrokes", true, 0.01f, 0.72f); }

    @Override
    protected int[] draw(DrawContext c, boolean editing) {
        long now = System.currentTimeMillis();
        dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;

        int k = 22, g = 2;
        int w = k * 3 + g * 2;
        var o = mc.options;
        key(c, "W", o.forwardKey, k + g, 0, k, k);
        key(c, "A", o.leftKey, 0, k + g, k, k);
        key(c, "S", o.backKey, k + g, k + g, k, k);
        key(c, "D", o.rightKey, (k + g) * 2, k + g, k, k);
        int y = (k + g) * 2;
        if (showMouse.value) {
            int mw = (w - g) / 2;
            mouse(c, "LMB", o.attackKey, CpsModule.left(), 0, y, mw, k);
            mouse(c, "RMB", o.useKey, CpsModule.right(), mw + g, y, w - mw - g, k);
            y += k + g;
        }
        if (showSpace.value) {
            key(c, "", o.jumpKey, 0, y, w, 12);
            float a = glow.getOrDefault("jump", 0f);
            c.fill(w / 2 - 12, y + 5, w / 2 + 12, y + 7, argb(1f, lerpColor(0xFFFFFF, 0x0D0414, a)));
            y += 12;
        }
        return new int[]{w, y};
    }

    private float press(String id, KeyBinding kb) {
        float cur = glow.getOrDefault(id, 0f);
        cur = damp(cur, kb.isPressed() ? 1f : 0f, 22f, dt);
        glow.put(id, cur);
        return cur;
    }

    private void key(DrawContext c, String label, KeyBinding kb, int x, int y, int w, int h) {
        float a = press(label.isEmpty() ? "jump" : label, kb);
        box(c, x, y, w, h, a);
        if (!label.isEmpty()) {
            var tr = mc.textRenderer;
            int col = argb(1f, lerpColor(color.rgb(), 0x0D0414, a));
            c.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2, col, false);
        }
    }

    private void mouse(DrawContext c, String label, KeyBinding kb, int cps, int x, int y, int w, int h) {
        float a = press(label, kb);
        box(c, x, y, w, h, a);
        var tr = mc.textRenderer;
        int col = argb(1f, lerpColor(color.rgb(), 0x0D0414, a));
        String s = cps + " CPS";
        c.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + 3, col, false);
        c.getMatrices().push();
        c.getMatrices().translate(x + w / 2f, y + 13, 0);
        c.getMatrices().scale(0.75f, 0.75f, 1f);
        c.drawText(tr, s, -tr.getWidth(s) / 2, 0, argb(0.85f, lerpColor(MUTE, 0x2A1040, a)), false);
        c.getMatrices().pop();
    }

    private void box(DrawContext c, int x, int y, int w, int h, float a) {
        if (background.value || a > 0.01f) {
            int bg = lerpColor(SLAB, NEON, a);
            c.fill(x, y, x + w, y + h, argb(background.value ? 0.55f + 0.4f * a : a * 0.95f, bg));
        }
    }
}
