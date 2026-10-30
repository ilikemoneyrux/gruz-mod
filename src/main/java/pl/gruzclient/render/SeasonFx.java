package pl.gruzclient.render;

import net.minecraft.client.gui.DrawContext;
import pl.gruzclient.cosmetics.CosmeticSet;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import static pl.gruzclient.render.GruzDraw.*;

/** Efekty sezonowe w menu: nietoperze i pomarańczowa poświata (Halloween), padający śnieg (Święta). */
public final class SeasonFx {
    private SeasonFx() {}

    private static final Random R = new Random(31);
    private static final float[][] BATS = new float[6][5];  // x, y, prędkość, skala, faza
    private static final float[][] SNOW = new float[90][4]; // x, y, prędkość, rozmiar
    private static boolean init = false;

    private static void setup() {
        if (init) return;
        init = true;
        for (float[] b : BATS) { b[0] = R.nextFloat(); b[1] = 0.08f + R.nextFloat() * 0.5f; b[2] = 0.03f + R.nextFloat() * 0.04f; b[3] = 0.6f + R.nextFloat() * 0.7f; b[4] = R.nextFloat() * 6f; }
        for (float[] s : SNOW) { s[0] = R.nextFloat(); s[1] = R.nextFloat(); s[2] = 0.03f + R.nextFloat() * 0.06f; s[3] = 1 + R.nextInt(2); }
    }

    /** Ile dni zostało do końca wydarzenia */
    public static long daysLeft() {
        String s = CosmeticSet.season();
        if (s == null) return 0;
        LocalDate now = LocalDate.now();
        LocalDate end = "halloween".equals(s) ? LocalDate.of(now.getYear(), 11, 10)
                : LocalDate.of(now.getMonthValue() == 12 ? now.getYear() + 1 : now.getYear(), 1, 6);
        return Math.max(0, ChronoUnit.DAYS.between(now, end) + 1);
    }

    public static void render(DrawContext c, int w, int h, float dt, float alpha) {
        String s = CosmeticSet.season();
        if (s == null || alpha <= 0.02f) return;
        setup();
        float t = seconds();
        if ("halloween".equals(s)) {
            c.fillGradient(0, h / 2, w, h, 0x00000000, argb(0.22f * alpha, 0xFF7A1A));
            for (float[] b : BATS) {
                b[0] += b[2] * dt;
                if (b[0] > 1.15f) { b[0] = -0.15f; b[1] = 0.08f + R.nextFloat() * 0.5f; }
                float x = b[0] * w, y = b[1] * h + (float) Math.sin(t * 1.3f + b[4]) * 8f;
                bat(c, x, y, b[3] * 1.6f, (float) Math.sin(t * 14f + b[4]), alpha);
            }
        } else {
            for (float[] f : SNOW) {
                f[1] += f[2] * dt;
                f[0] += (float) Math.sin(t * 0.7f + f[3] * 3f + f[1] * 6f) * 0.0006f;
                if (f[1] > 1.02f) { f[1] = -0.02f; f[0] = R.nextFloat(); }
                int x = Math.round(f[0] * w), y = Math.round(f[1] * h), z = (int) f[3];
                c.fill(x, y, x + z, y + z, argb(0.75f * alpha, 0xFFFFFF));
            }
        }
    }

    /** Pikselowy nietoperz (ciało + machające skrzydła) */
    private static void bat(DrawContext c, float cx, float cy, float s, float flap, float alpha) {
        int col = argb(0.85f * alpha, 0x0A0610);
        int x = Math.round(cx), y = Math.round(cy);
        int k = Math.max(1, Math.round(s));
        c.fill(x - k, y - k, x + k, y + 2 * k, col);                 // ciało
        c.fill(x - k, y - 2 * k, x - k / 2 - 0, y - k, col);           // uszy
        c.fill(x + k / 2, y - 2 * k, x + k, y - k, col);
        int lift = Math.round(flap * 2 * k);
        for (int i = 1; i <= 4; i++) {                                  // skrzydła schodkami
            int wy = y - lift * i / 4;
            c.fill(x - k - i * 2 * k, wy, x - k - (i - 1) * 2 * k, wy + k + (i < 3 ? k : 0), col);
            c.fill(x + k + (i - 1) * 2 * k, wy, x + k + i * 2 * k, wy + k + (i < 3 ? k : 0), col);
        }
    }
}
