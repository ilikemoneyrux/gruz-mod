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

    // ---------------- od 1.21.9: gra sama rysuje grube linie, my podmieniamy kolor/grubość i dokładamy wypełnienie ----------------

    /** Kolor obrysu ARGB zamiast domyślnego (czarny, 40%) */
    public int outlineArgb(int vanilla) {
        if (!isEnabled()) return vanilla;
        int a = Math.round(Math.max(0f, Math.min(1f, outlineAlpha.value / 100f)) * 255f);
        return (a << 24) | color.rgb();
    }

    /** Grubość linii: ustawienie 1 = jak w grze */
    public float lineWidth(float vanilla) {
        return isEnabled() ? vanilla * thickness.value : vanilla;
    }

    public boolean wantsFill() { return isEnabled() && fill.value; }

    //#if MC >= 1.21.9

    /** Półprzezroczyste ścianki bloku (QUADS, pozycja + kolor), współrzędne względem kamery */
    public void fillShape(com.mojang.blaze3d.vertex.PoseStack.Pose p, com.mojang.blaze3d.vertex.VertexConsumer vc,
                          net.minecraft.world.phys.shapes.VoxelShape shape, double ox, double oy, double oz) {
        float[] c = rgba(fillAlpha.value / 100f * pulseFactor());
        for (net.minecraft.world.phys.AABB b : shape.toAabbs()) {
            float x1 = (float) (b.minX + ox - 0.002), y1 = (float) (b.minY + oy - 0.002), z1 = (float) (b.minZ + oz - 0.002);
            float x2 = (float) (b.maxX + ox + 0.002), y2 = (float) (b.maxY + oy + 0.002), z2 = (float) (b.maxZ + oz + 0.002);
            quad(p, vc, c, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2); // dół
            quad(p, vc, c, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1); // góra
            quad(p, vc, c, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1); // północ
            quad(p, vc, c, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2); // południe
            quad(p, vc, c, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1); // zachód
            quad(p, vc, c, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2); // wschód
        }
    }

    private static void quad(com.mojang.blaze3d.vertex.PoseStack.Pose p, com.mojang.blaze3d.vertex.VertexConsumer vc, float[] c,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vc.addVertex(p, ax, ay, az).setColor(c[0], c[1], c[2], c[3]);
        vc.addVertex(p, bx, by, bz).setColor(c[0], c[1], c[2], c[3]);
        vc.addVertex(p, cx, cy, cz).setColor(c[0], c[1], c[2], c[3]);
        vc.addVertex(p, dx, dy, dz).setColor(c[0], c[1], c[2], c[3]);
    }
    //#endif
}
