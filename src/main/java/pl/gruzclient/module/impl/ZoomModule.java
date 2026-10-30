package pl.gruzclient.module.impl;

import org.lwjgl.glfw.GLFW;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;

/** Zoom pod klawiszem (domyślnie C). Kółko myszy w trakcie zooma zmienia przybliżenie. */
public class ZoomModule extends Module {
    public final Setting.Key key = add(new Setting.Key("key", "setting.key", GLFW.GLFW_KEY_C));
    public final Setting.Choice mode = add(new Setting.Choice("mode", "setting.mode", 0, "setting.mode.hold", "setting.mode.toggle"));
    public final Setting.Number level = add(new Setting.Number("level", "setting.level", 4f, 2f, 12f, 1f, "x"));
    public final Setting.Bool smooth = add(new Setting.Bool("smooth", "setting.smooth", true));

    private boolean active, wasDown;
    private float scrollLevel = -1f;
    private float current = 1f;
    private long last = 0;

    public ZoomModule() { super("zoom", Category.UTILITY, true); }

    @Override
    public void tick() {
        boolean down = Modules.keyDown(key.code);
        if (mode.index == 0) active = down;
        else if (down && !wasDown) active = !active;
        wasDown = down;
        if (!active) scrollLevel = -1f;
    }

    public boolean isZooming() { return isEnabled() && active; }

    /** Wywoływane z kółka myszy – zwraca true, gdy przewinięcie zostało użyte przez zoom */
    public boolean onScroll(double amount) {
        if (!isZooming()) return false;
        float base = scrollLevel < 0 ? level.value : scrollLevel;
        scrollLevel = Math.max(1.5f, Math.min(40f, base * (amount > 0 ? 1.2f : 1f / 1.2f)));
        return true;
    }

    /** Mnożnik dzielący pole widzenia; liczony co klatkę, płynnie */
    public float factor() {
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        float target = isZooming() ? (scrollLevel < 0 ? level.value : scrollLevel) : 1f;
        if (!smooth.value) current = target;
        else current += (target - current) * (1f - (float) Math.exp(-14f * dt));
        return current;
    }

    @Override
    protected void onDisable() { active = false; current = 1f; }
}
