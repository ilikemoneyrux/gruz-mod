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

    /** "GRUZX1:" + base64 z "styl;kolor;rozmiar;przerwa;grubość;obrys;kolorTrafienia(on);kolorTrafienia;puls;dynamiczny;kodPikseli" */
    public String encodeAll() {
        String s = String.join(";", String.valueOf(style.index), String.valueOf(color.index), String.valueOf(Math.round(size.value)),
                String.valueOf(Math.round(gap.value)), String.valueOf(Math.round(thickness.value)), outline.value ? "1" : "0",
                hitColorOn.value ? "1" : "0", String.valueOf(hitColor.index), pulse.value ? "1" : "0", dynamic.value ? "1" : "0", pixels.value);
        return "GRUZX1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Wczytuje kod od innego gracza; liczby są przycinane do zakresów ustawień. false = to nie jest kod celownika. */
    public boolean decodeAll(String code) {
        if (code == null || !code.startsWith("GRUZX1:") || code.length() > 2048) return false;
        try {
            String[] f = new String(Base64.getUrlDecoder().decode(code.substring(7).trim()), java.nio.charset.StandardCharsets.UTF_8).split(";", -1);
            if (f.length != 11 || decode(f[10]) == null) return false;
            style.index = Math.floorMod(Integer.parseInt(f[0]), style.keys.length);
            color.index = Math.floorMod(Integer.parseInt(f[1]), color.keys.length);
            size.set(Integer.parseInt(f[2])); gap.set(Integer.parseInt(f[3])); thickness.set(Integer.parseInt(f[4]));
            outline.value = "1".equals(f[5]); hitColorOn.value = "1".equals(f[6]);
            hitColor.index = Math.floorMod(Integer.parseInt(f[7]), hitColor.keys.length);
            pulse.value = "1".equals(f[8]); dynamic.value = "1".equals(f[9]); pixels.value = f[10];
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

    /** Celownik ze środkiem w (cx, cy) – ten sam kod rysuje podgląd w ustawieniach. Puls po trafieniu = chwilowe powiększenie. */
    public void draw(GuiGraphics c, int cx, int cy) {
        long since = System.currentTimeMillis() - pl.gruzclient.combat.Combat.lastHitAt;
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

    private void drawShape(GuiGraphics c, int cx, int cy, long sinceHit) {
        int col = 0xFF000000 | (hitColorOn.value && sinceHit < 250 ? hitColor.rgb() : color.rgb());
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
        int s = Math.round(size.value), gp = Math.round(gap.value) + dynamicGap(), t = Math.round(thickness.value);
        int lo = -(t / 2), hi = lo + t;
        if (style.index == 0 || style.index == 2) {
            if (outline.value) {
                c.fill(cx + lo - 1, cy + lo - gp - s - 1, cx + hi + 1, cy + lo - gp + 1, shadow);
                c.fill(cx + lo - 1, cy + hi + gp - 1, cx + hi + 1, cy + hi + gp + s + 1, shadow);
                c.fill(cx + lo - gp - s - 1, cy + lo - 1, cx + lo - gp + 1, cy + hi + 1, shadow);
                c.fill(cx + hi + gp - 1, cy + lo - 1, cx + hi + gp + s + 1, cy + hi + 1, shadow);
            }
            // ramiona liczone od krawędzi środkowego kwadratu [lo, hi) – tak samo daleko w każdą stronę
            c.fill(cx + lo, cy + lo - gp - s, cx + hi, cy + lo - gp, col);
            c.fill(cx + lo, cy + hi + gp, cx + hi, cy + hi + gp + s, col);
            c.fill(cx + lo - gp - s, cy + lo, cx + lo - gp, cy + hi, col);
            c.fill(cx + hi + gp, cy + lo, cx + hi + gp + s, cy + hi, col);
            if (gp == 0 && style.index == 0) c.fill(cx + lo, cy + lo, cx + hi, cy + hi, col); // bez przerwy: pełny krzyż bez dziury w środku
        }
        if (style.index == 1 || style.index == 2) {
            if (outline.value) c.fill(cx + lo - 1, cy + lo - 1, cx + hi + 1, cy + hi + 1, shadow);
            c.fill(cx + lo, cy + lo, cx + hi, cy + hi, col);
        }
    }
}
