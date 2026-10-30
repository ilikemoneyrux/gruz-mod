package pl.gruzclient.module.impl;

import net.minecraft.client.gui.DrawContext;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

import static pl.gruzclient.render.GruzDraw.*;

/** Delikatna czerwona winieta na krawędziach ekranu, gdy masz mało życia. Pulsuje coraz szybciej. */
public class LowHpModule extends Module {
    public final Setting.Number threshold = add(new Setting.Number("threshold", "setting.hpThreshold", 6f, 2f, 12f, 1f, " HP"));
    public final Setting.Number strength = add(new Setting.Number("strength", "setting.strength", 60f, 20f, 100f, 5f, "%"));

    private float shown = 0f;
    private long last = 0;

    public LowHpModule() { super("lowhp", Category.PVP, true); }

    public void render(DrawContext c) {
        if (!isEnabled() || mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        float hp = mc.player.getHealth();
        float target = hp <= threshold.value ? 1f - (hp / threshold.value) * 0.6f : 0f;
        shown = damp(shown, target, 6f, dt);
        if (shown < 0.01f) return;
        float speed = 2f + 4f * shown;
        float pulse = 0.75f + 0.25f * (float) Math.sin(seconds() * speed);
        float a = shown * pulse * strength.value / 100f;
        int w = c.getScaledWindowWidth(), h = c.getScaledWindowHeight();
        int edge = Math.max(24, Math.round(Math.min(w, h) * 0.16f));
        int red = 0xC4224F;
        int on = argb(a * 0.85f, red), off = argb(0f, red);
        c.fillGradient(0, 0, w, edge, on, off);                 // góra
        c.fillGradient(0, h - edge, w, h, off, on);             // dół
        // boki: fillGradient jest pionowy, więc rysujemy paski o rosnącej przezroczystości
        int steps = 12;
        for (int i = 0; i < steps; i++) {
            float k = 1f - (float) i / steps;
            int col = argb(a * 0.85f * k * k / 2.2f, red);
            int x0 = Math.round((float) edge * i / steps), x1 = Math.round((float) edge * (i + 1) / steps);
            c.fill(x0, 0, x1, h, col);
            c.fill(w - x1, 0, w - x0, h, col);
        }
    }
}
