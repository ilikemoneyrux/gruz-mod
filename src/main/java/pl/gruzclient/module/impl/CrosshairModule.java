package pl.gruzclient.module.impl;

import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;
import pl.gruzclient.screen.CrosshairEditorScreen;

import java.util.Base64;
import net.minecraft.client.gui.GuiGraphics;

/** Własny celownik: gotowe kształty albo rysunek piksel po pikselu (z kodem do udostępniania). */
public class CrosshairModule extends Module {
    public static final int GRID = 15;

    public final Setting.Choice style = add(new Setting.Choice("style", "setting.style", 0,
            "setting.style.cross", "setting.style.dot", "setting.style.crossdot", "setting.style.pixel"));
    public final Setting.Color color = add(new Setting.Color("color", "setting.color", 1));
    public final Setting.Number size = add(new Setting.Number("size", "setting.size", 5f, 2f, 12f, 1f, ""));
    public final Setting.Number gap = add(new Setting.Number("gap", "setting.gap", 2f, 0f, 8f, 1f, ""));
    public final Setting.Number thickness = add(new Setting.Number("thickness", "setting.thickness", 1f, 1f, 3f, 1f, ""));
    public final Setting.Bool outline = add(new Setting.Bool("outline", "setting.crosshairOutline", true));
    public final Setting.Text pixels = add(new Setting.Text("pixels", "setting.pixelCode", defaultCode()));
    public final Setting.Action edit = add(new Setting.Action("edit", "setting.editPixels",
            () -> mc.setScreen(new CrosshairEditorScreen(mc.screen, this))));

    private String decodedFrom = null;
    private boolean[] grid = new boolean[GRID * GRID];

    public CrosshairModule() { super("crosshair", Category.VISUAL, false); }

    // ---------------- kod celownika ----------------

    /** "GRUZ1:" + base64 z 225 bitów (wiersz po wierszu) */
    public static String encode(boolean[] g) {
        byte[] bytes = new byte[(GRID * GRID + 7) / 8];
        for (int i = 0; i < g.length; i++) if (g[i]) bytes[i >> 3] |= (byte) (1 << (i & 7));
        return "GRUZ1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static boolean[] decode(String code) {
        boolean[] g = new boolean[GRID * GRID];
        if (code == null || !code.startsWith("GRUZ1:")) return null;
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(code.substring(6).trim());
            for (int i = 0; i < g.length && (i >> 3) < bytes.length; i++) g[i] = (bytes[i >> 3] & (1 << (i & 7))) != 0;
            return g;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String defaultCode() {
        boolean[] g = new boolean[GRID * GRID];
        int c = GRID / 2;
        for (int i = 0; i < GRID; i++) {
            if (Math.abs(i - c) >= 2 && Math.abs(i - c) <= 5) { g[c * GRID + i] = true; g[i * GRID + c] = true; }
        }
        g[c * GRID + c] = true;
        return encode(g);
    }

    public boolean[] grid() {
        if (!pixels.value.equals(decodedFrom)) {
            boolean[] g = decode(pixels.value);
            grid = g != null ? g : decode(defaultCode());
            decodedFrom = pixels.value;
        }
        return grid;
    }

    // ---------------- rysowanie ----------------

    public void draw(GuiGraphics c) {
        int cx = c.guiWidth() / 2, cy = c.guiHeight() / 2;
        int col = 0xFF000000 | color.rgb();
        int shadow = 0x90000000;
        if (style.index == 3) {
            boolean[] g = grid();
            int half = GRID / 2;
            if (outline.value) {
                for (int y = 0; y < GRID; y++) for (int x = 0; x < GRID; x++) {
                    if (g[y * GRID + x]) c.fill(cx + x - half - 1, cy + y - half - 1, cx + x - half + 2, cy + y - half + 2, shadow);
                }
            }
            for (int y = 0; y < GRID; y++) for (int x = 0; x < GRID; x++) {
                if (g[y * GRID + x]) c.fill(cx + x - half, cy + y - half, cx + x - half + 1, cy + y - half + 1, col);
            }
            return;
        }
        int s = Math.round(size.value), gp = Math.round(gap.value), t = Math.round(thickness.value);
        int lo = -(t / 2), hi = lo + t;
        if (style.index == 0 || style.index == 2) {
            if (outline.value) {
                c.fill(cx + lo - 1, cy - gp - s - 1, cx + hi + 1, cy - gp + 1, shadow);
                c.fill(cx + lo - 1, cy + gp - 1, cx + hi + 1, cy + gp + s + 1, shadow);
                c.fill(cx - gp - s - 1, cy + lo - 1, cx - gp + 1, cy + hi + 1, shadow);
                c.fill(cx + gp - 1, cy + lo - 1, cx + gp + s + 1, cy + hi + 1, shadow);
            }
            c.fill(cx + lo, cy - gp - s, cx + hi, cy - gp, col);
            c.fill(cx + lo, cy + gp, cx + hi, cy + gp + s, col);
            c.fill(cx - gp - s, cy + lo, cx - gp, cy + hi, col);
            c.fill(cx + gp, cy + lo, cx + gp + s, cy + hi, col);
        }
        if (style.index == 1 || style.index == 2) {
            if (outline.value) c.fill(cx + lo - 1, cy + lo - 1, cx + hi + 1, cy + hi + 1, shadow);
            c.fill(cx + lo, cy + lo, cx + hi, cy + hi, col);
        }
    }
}
