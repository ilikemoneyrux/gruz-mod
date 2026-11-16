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
            "setting.style.cross", "setting.style.dot", "setting.style.crossdot", "setting.style.pixel",
            "setting.style.t", "setting.style.x", "setting.style.circle", "setting.style.square"));
    public final Setting.Color color = add(new Setting.Color("color", "setting.color", 1));
    public final Setting.Number size = add(new Setting.Number("size", "setting.size", 5f, 2f, 12f, 1f, ""));
    public final Setting.Number gap = add(new Setting.Number("gap", "setting.gap", 2f, 0f, 8f, 1f, ""));
    public final Setting.Number thickness = add(new Setting.Number("thickness", "setting.thickness", 1f, 1f, 3f, 1f, ""));
    public final Setting.Bool outline = add(new Setting.Bool("outline", "setting.crosshairOutline", true));
    public final Setting.Number outlineWidth = add(new Setting.Number("outlineWidth", "setting.crosshairOutlineWidth", 1f, 1f, 3f, 1f, " px"));
    public final Setting.Number dotSize = add(new Setting.Number("dotSize", "setting.crosshairDotSize", 1f, 1f, 6f, 1f, ""));
    public final Setting.Number opacity = add(new Setting.Number("opacity", "setting.opacity", 100f, 10f, 100f, 5f, "%"));
    public final Setting.Text pixels = add(new Setting.Text("pixels", "setting.pixelCode", defaultCode()));
    public final Setting.Action edit = add(new Setting.Action("edit", "setting.editPixels",
            () -> mc.setScreen(new CrosshairEditorScreen(mc.screen, this))));
    /** Gotowe celowniki PvP – wybór od razu ustawia kształt (dalej można go zmieniać) */
    public final Setting.Choice preset = add(new Setting.Choice("preset", "setting.crosshairPreset", 0,
            "xpreset.custom", "xpreset.classic", "xpreset.dot", "xpreset.tiny", "xpreset.thick", "xpreset.wide"));
    public final Setting.Bool hitColorOn = add(new Setting.Bool("hitColor", "setting.crosshairHitColor", false));
    public final Setting.Color hitColor = add(new Setting.Color("hitColorValue", "setting.crosshairHitColorValue", 5));
    public final Setting.Bool pulse = add(new Setting.Bool("hitPulse", "setting.crosshairPulse", false));
    public final Setting.Bool dynamic = add(new Setting.Bool("dynamic", "setting.crosshairDynamic", false));
    public final Setting.Action copyCode = add(new Setting.Action("copyCode", "setting.crosshairCopy", this::copyCode).button("xcode.copy"));
    public final Setting.Action pasteCode = add(new Setting.Action("pasteCode", "setting.crosshairPaste", this::pasteCode).button("xcode.paste"));

    private String decodedFrom = null;
    private float spread = 0;
    private boolean[] grid = new boolean[GRID * GRID];

    public CrosshairModule() {
        super("crosshair", Category.VISUAL, false);
        preset.onChange = () -> applyPreset(preset.index); // działa też, gdy moduł jest wyłączony
    }

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

    // ---------------- presety i kod całego celownika ----------------

    /** {styl, rozmiar, przerwa, grubość, obrys} */
    private static final int[][] PRESETS = {{}, {0, 5, 2, 1, 1}, {1, 2, 0, 2, 1}, {0, 3, 1, 1, 1}, {2, 6, 3, 2, 1}, {0, 9, 4, 1, 0}};

    private void applyPreset(int i) {
        if (i <= 0 || i >= PRESETS.length) return;
        int[] p = PRESETS[i];
        style.index = p[0]; size.set(p[1]); gap.set(p[2]); thickness.set(p[3]); outline.value = p[4] == 1;
        pl.gruzclient.module.Modules.save();
    }

    /** "GRUZX1:" + base64 z "styl;kolor;rozmiar;przerwa;grubość;obrys;kolorTrafienia(on);kolorTrafienia;puls;dynamiczny;kodPikseli;grubośćObrysu;kropka;krycie" */
    public String encodeAll() {
        String s = String.join(";", String.valueOf(style.index), String.valueOf(color.index), String.valueOf(Math.round(size.value)),
                String.valueOf(Math.round(gap.value)), String.valueOf(Math.round(thickness.value)), outline.value ? "1" : "0",
                hitColorOn.value ? "1" : "0", String.valueOf(hitColor.index), pulse.value ? "1" : "0", dynamic.value ? "1" : "0", pixels.value,
                String.valueOf(Math.round(outlineWidth.value)), String.valueOf(Math.round(dotSize.value)), String.valueOf(Math.round(opacity.value)));
        return "GRUZX1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Wczytuje kod od innego gracza; liczby są przycinane do zakresów ustawień. false = to nie jest kod celownika. */
    public boolean decodeAll(String code) {
        if (code == null || !code.startsWith("GRUZX1:") || code.length() > 2048) return false;
        try {
            String[] f = new String(Base64.getUrlDecoder().decode(code.substring(7).trim()), java.nio.charset.StandardCharsets.UTF_8).split(";", -1);
            if ((f.length != 11 && f.length != 14) || decode(f[10]) == null) return false; // 11 pól = kod sprzed 0.5.23
            style.index = Math.floorMod(Integer.parseInt(f[0]), style.keys.length);
            color.index = Math.floorMod(Integer.parseInt(f[1]), color.keys.length);
            size.set(Integer.parseInt(f[2])); gap.set(Integer.parseInt(f[3])); thickness.set(Integer.parseInt(f[4]));
            outline.value = "1".equals(f[5]); hitColorOn.value = "1".equals(f[6]);
            hitColor.index = Math.floorMod(Integer.parseInt(f[7]), hitColor.keys.length);
            pulse.value = "1".equals(f[8]); dynamic.value = "1".equals(f[9]); pixels.value = f[10];
            if (f.length == 14) { outlineWidth.set(Integer.parseInt(f[11])); dotSize.set(Integer.parseInt(f[12])); opacity.set(Integer.parseInt(f[13])); }
            else { outlineWidth.set(1); dotSize.set(1); opacity.set(100); }
        } catch (IllegalArgumentException e) { // zły base64 albo nie-liczba
            return false;
        }
        preset.index = 0; // własny
        pl.gruzclient.module.Modules.save();
        return true;
    }

    private void copyCode() {
        mc.keyboardHandler.setClipboard(encodeAll());
        say("xcode.copied");
    }

    private void pasteCode() {
        say(decodeAll(String.valueOf(mc.keyboardHandler.getClipboard()).trim()) ? "xcode.loaded" : "xcode.bad");
    }

    private void say(String key) {
        if (mc.player != null) pl.gruzclient.compat.Compat.chat(mc.player, net.minecraft.network.chat.Component.literal("§d[Gruz] §f" + pl.gruzclient.i18n.Lang.t(key)), true);
        else pl.gruzclient.GruzClient.LOG.info("Crosshair: {}", key);
    }

    // ---------------- rysowanie ----------------

    public void draw(GuiGraphics c) { draw(c, c.guiWidth() / 2, c.guiHeight() / 2); }

    /** Celownik ze środkiem w (cx, cy). Puls po trafieniu = chwilowe powiększenie. */
    public void draw(GuiGraphics c, int cx, int cy) { draw(c, cx, cy, System.currentTimeMillis() - pl.gruzclient.combat.Combat.lastHitAt); }

    /** sinceHit = ms od trafienia (podgląd w ustawieniach podaje udawane trafienia, żeby było widać kolor i puls) */
    public void draw(GuiGraphics c, int cx, int cy, long since) {
        float k = pulse.value && since < 200 ? 1f + 0.35f * (1f - since / 200f) : 1f;
        if (k <= 1.001f) { drawShape(c, cx, cy, since); return; }
        pl.gruzclient.compat.Pose2D p = pl.gruzclient.compat.Pose2D.of(c);
        p.push(); p.translate(cx, cy); p.scale(k, k);
        drawShape(c, 0, 0, since);
        p.pop();
    }

    /** Dynamiczny rozmiar: przerwa rośnie przy ruchu, w powietrzu i gdy atak się ładuje */
    private int dynamicGap() {
        float want = 0;
        var pl = mc.player;
        if (dynamic.value && pl != null) {
            if (pl.isSprinting()) want += 3;
            else if (pl.getDeltaMovement().horizontalDistanceSqr() > 0.002) want += 1.5f;
            if (!pl.onGround()) want += 2;
            want += (1f - pl.getAttackStrengthScale(0f)) * 4f;
        }
        spread += (want - spread) * 0.25f; // płynne przejście zamiast skoków
        return Math.round(spread);
    }

    // Kształt jako maska pikseli; obwódka = maska poszerzona o grubość obrysu, więc półprzezroczysty cień nigdy się nie nakłada
    private boolean[] mask, ring;
    private int maskR = -1, maskKey = 0;

    private void drawShape(GuiGraphics c, int cx, int cy, long sinceHit) {
        float a = opacity.value / 100f;
        int col = pl.gruzclient.render.GruzDraw.argb(a, hitColorOn.value && sinceHit < 250 ? hitColor.rgb() : color.rgb());
        int shadow = pl.gruzclient.render.GruzDraw.argb(a * 0.565f, 0); // 0x90 jak wcześniej
        int s = Math.round(size.value), gp = Math.round(gap.value) + (style.index == 3 ? 0 : dynamicGap()), t = Math.round(thickness.value);
        int ow = outline.value ? Math.round(outlineWidth.value) : 0, dot = Math.max(t, Math.round(dotSize.value));
        int key = java.util.Objects.hash(style.index, s, gp, t, ow, dot, style.index == 3 ? pixels.value : "");
        if (mask == null || key != maskKey) build(s, gp, t, ow, dot, key);
        int r = maskR, n = 2 * r + 1;
        if (ow > 0) runs(c, ring, n, cx - r, cy - r, shadow);
        runs(c, mask, n, cx - r, cy - r, col);
    }

    /** Rysuje maskę poziomymi odcinkami (jeden fill na odcinek wiersza) */
    private static void runs(GuiGraphics c, boolean[] m, int n, int ox, int oy, int argb) {
        for (int y = 0; y < n; y++) {
            int x = 0;
            while (x < n) {
                if (!m[y * n + x]) { x++; continue; }
                int x0 = x;
                while (x < n && m[y * n + x]) x++;
                c.fill(ox + x0, oy + y, ox + x, oy + y + 1, argb);
            }
        }
    }

    /** Buduje maskę (2r+1)² ze środkiem w r; współrzędne kształtu są względem (cx, cy) jak w starym rysowaniu fill */
    private void build(int s, int gp, int t, int ow, int dot, int key) {
        int r = Math.max(GRID / 2 + 1, gp + s + t + 2) + ow, n = 2 * r + 1;
        boolean[] m = new boolean[n * n];
        int lo = -(t / 2), hi = lo + t, st = style.index;
        // ramiona liczone od krawędzi środkowego kwadratu [lo, hi) – tak samo daleko w każdą stronę
        if (st == 0 || st == 2 || st == 4) {
            if (st != 4) box(m, r, lo, lo - gp - s, hi, lo - gp); // góra (T = bez górnego ramienia)
            box(m, r, lo, hi + gp, hi, hi + gp + s);
            box(m, r, lo - gp - s, lo, lo - gp, hi);
            box(m, r, hi + gp, lo, hi + gp + s, hi);
            if (gp == 0 && st != 2) box(m, r, lo, lo, hi, hi); // bez przerwy: pełny krzyż bez dziury w środku
        }
        if (st == 3) {
            boolean[] g = grid();
            int half = GRID / 2;
            for (int y = 0; y < GRID; y++) for (int x = 0; x < GRID; x++) if (g[y * GRID + x]) box(m, r, x - half, y - half, x - half + 1, y - half + 1);
        }
        if (st == 5) { // X: kwadraty t×t po przekątnych, symetrycznie względem [lo, hi)
            for (int i = gp + 1; i <= gp + s; i++) {
                box(m, r, lo + i, lo + i, hi + i, hi + i); box(m, r, lo - i, lo + i, hi - i, hi + i);
                box(m, r, lo + i, lo - i, hi + i, hi - i); box(m, r, lo - i, lo - i, hi - i, hi - i);
            }
            if (gp == 0) box(m, r, lo, lo, hi, hi);
        }
        if (st == 6) { // okrąg o promieniu przerwa + rozmiar, grubość t, środek w środku kwadratu [lo, hi)
            float mid = (lo + hi) / 2f, out = gp + s + 0.5f, in = out - t;
            for (int y = -r; y <= r; y++) for (int x = -r; x <= r; x++) {
                double d = Math.hypot(x + 0.5f - mid, y + 0.5f - mid);
                if (d <= out && d > in) m[(y + r) * n + x + r] = true;
            }
        }
        if (st == 7) { // ramka kwadratu: krawędź zewnętrzna przerwa + rozmiar od środka
            int R = gp + s;
            box(m, r, lo - R, lo - R, hi + R, lo - R + t); box(m, r, lo - R, hi + R - t, hi + R, hi + R);
            box(m, r, lo - R, lo - R + t, lo - R + t, hi + R - t); box(m, r, hi + R - t, lo - R + t, hi + R, hi + R - t);
        }
        // kropka: w stylach z kropką zawsze, w okręgu i kwadracie gdy rozmiar kropki > 1
        if (st == 1 || st == 2 || ((st == 6 || st == 7) && Math.round(dotSize.value) > 1)) {
            int dl = -(dot / 2);
            box(m, r, dl, dl, dl + dot, dl + dot);
        }
        boolean[] o = new boolean[n * n];
        if (ow > 0) for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            if (m[y * n + x]) continue;
            search:
            for (int dy = -ow; dy <= ow; dy++) for (int dx = -ow; dx <= ow; dx++) {
                int xx = x + dx, yy = y + dy;
                if (xx >= 0 && yy >= 0 && xx < n && yy < n && m[yy * n + xx]) { o[y * n + x] = true; break search; }
            }
        }
        mask = m; ring = o; maskR = r; maskKey = key;
    }

    /** Prostokąt [x1, x2) × [y1, y2) we współrzędnych względem środka, przycięty do maski */
    private static void box(boolean[] m, int r, int x1, int y1, int x2, int y2) {
        int n = 2 * r + 1;
        for (int y = Math.max(-r, y1); y < Math.min(r + 1, y2); y++)
            for (int x = Math.max(-r, x1); x < Math.min(r + 1, x2); x++) m[(y + r) * n + x + r] = true;
    }
}
