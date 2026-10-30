package pl.gruzclient.module.impl;

import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/** Kolorowy obrys bloku, na który patrzysz, z opcjonalnym półprzezroczystym wypełnieniem. */
public class BlockOverlayModule extends Module {
    public final Setting.Color color = add(new Setting.Color("color", "setting.color", 1));
    public final Setting.Number outlineAlpha = add(new Setting.Number("outline", "setting.outlineOpacity", 90f, 10f, 100f, 5f, "%"));
    public final Setting.Bool fill = add(new Setting.Bool("fill", "setting.fill", true));
    public final Setting.Number fillAlpha = add(new Setting.Number("fillAlpha", "setting.fillOpacity", 18f, 5f, 60f, 1f, "%"));
    public final Setting.Number thickness = add(new Setting.Number("thickness", "setting.lineWidth", 1f, 1f, 8f, 1f, " px"));
    public final Setting.Bool pulse = add(new Setting.Bool("pulse", "setting.pulse", true));

    public BlockOverlayModule() { super("blockoverlay", Category.VISUAL, false); }

    public float[] rgba(float alpha) {
        int c = color.rgb();
        return new float[]{((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, alpha};
    }

    /** Grube linie rysujemy sami (krawędzie jako cienkie prostopadłościany), cienkie zostawiamy grze */
    public boolean thick() { return isEnabled() && thickness.value > 1.5f; }

    /** Lekkie "oddychanie" wypełnienia */
    public float pulseFactor() {
        if (!pulse.value) return 1f;
        return 0.75f + 0.25f * (float) Math.sin(System.currentTimeMillis() / 380.0);
    }
}
