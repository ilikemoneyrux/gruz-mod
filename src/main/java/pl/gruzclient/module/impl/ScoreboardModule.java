package pl.gruzclient.module.impl;

import net.minecraft.client.gui.DrawContext;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/** Tablica wyników z boku: rozmiar, przesunięcie, przezroczystość tła, ukrywanie czerwonych liczb. */
public class ScoreboardModule extends Module {
    public final Setting.Bool hide = add(new Setting.Bool("hide", "setting.hideScoreboard", false));
    public final Setting.Number size = add(new Setting.Number("size", "setting.size", 1f, 0.5f, 1.5f, 0.05f, "x"));
    public final Setting.Number opacity = add(new Setting.Number("opacity", "setting.bgOpacity", 30f, 0f, 100f, 5f, "%"));
    public final Setting.Bool numbers = add(new Setting.Bool("numbers", "setting.scoreNumbers", false));

    /** true w trakcie rysowania tablicy (żeby mixiny tła i liczb działały tylko tutaj) */
    public boolean drawing;
    /** przesunięcie w pikselach GUI (ustawiane przeciąganiem w edytorze HUD) */
    public float offX = 0, offY = 0;
    /** prostokąt tablicy w układzie gry (przed naszym przesunięciem), łapany z wywołań fill */
    private int rx1, ry1, rx2, ry2;
    public int[] lastRect = null;
    public long lastSeen = 0;

    public ScoreboardModule() { super("scoreboard", Category.VISUAL, true); }

    public boolean hidden() { return isEnabled() && hide.value; }

    public void begin(DrawContext c) {
        drawing = isEnabled();
        if (!drawing) return;
        var m = c.getMatrices();
        m.push();
        float w = c.getScaledWindowWidth(), h = c.getScaledWindowHeight();
        m.translate(w + offX, h / 2f + offY, 0);
        m.scale(size.value, size.value, 1f);
        m.translate(-w, -h / 2f, 0);
        rx1 = ry1 = Integer.MAX_VALUE;
        rx2 = ry2 = Integer.MIN_VALUE;
    }

    /** Wołane z DrawContextMixin dla każdego fill w trakcie rysowania tablicy */
    public void capture(int x1, int y1, int x2, int y2) {
        rx1 = Math.min(rx1, Math.min(x1, x2)); ry1 = Math.min(ry1, Math.min(y1, y2));
        rx2 = Math.max(rx2, Math.max(x1, x2)); ry2 = Math.max(ry2, Math.max(y1, y2));
    }

    /** Prostokąt na ekranie po przesunięciu i skali: {x, y, w, h} albo null */
    public int[] screenRect(int w, int h) {
        int[] r = lastRect != null && System.currentTimeMillis() - lastSeen < 3000 ? lastRect
                : new int[]{w - 100, h / 2 - 50, w - 2, h / 2 + 40}; // zastępczy, gdy serwer nie ma tablicy
        float k = size.value;
        int x1 = Math.round(w + offX + (r[0] - w) * k), y1 = Math.round(h / 2f + offY + (r[1] - h / 2f) * k);
        int x2 = Math.round(w + offX + (r[2] - w) * k), y2 = Math.round(h / 2f + offY + (r[3] - h / 2f) * k);
        return new int[]{x1, y1, x2 - x1, y2 - y1};
    }

    public boolean live() { return lastRect != null && System.currentTimeMillis() - lastSeen < 3000; }

    public void end(DrawContext c) {
        if (!drawing) return;
        drawing = false;
        if (rx2 > rx1) { lastRect = new int[]{rx1, ry1, rx2, ry2}; lastSeen = System.currentTimeMillis(); }
        c.getMatrices().pop();
    }

    /** Kolor tła wierszy; tytuł dostaje trochę mocniejsze tło jak w grze */
    public int background(float vanillaOpacity) {
        float a = opacity.value / 100f * (vanillaOpacity > 0.35f ? 1.33f : 1f);
        return Math.round(Math.min(1f, a) * 255f) << 24;
    }

    @Override
    public void load(com.google.gson.JsonObject o) {
        super.load(o);
        if (o.has("offX")) offX = o.get("offX").getAsFloat();
        if (o.has("offY")) offY = o.get("offY").getAsFloat();
    }

    @Override
    public com.google.gson.JsonObject save() {
        var o = super.save();
        o.addProperty("offX", offX);
        o.addProperty("offY", offY);
        return o;
    }

    public boolean hideNumbers() { return drawing && !numbers.value; }
}
