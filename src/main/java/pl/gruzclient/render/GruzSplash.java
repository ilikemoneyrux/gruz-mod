package pl.gruzclient.render;

import net.minecraft.client.gui.DrawContext;

import static pl.gruzclient.render.GruzDraw.*;

/** Ekran ładowania Gruz: czerń, logo z pulsującą poświatą, neonowy pasek postępu. */
public final class GruzSplash {
    private GruzSplash() {}

    private static float shownProgress = 0f;
    private static long last = 0;

    /**
     * @param alpha    widoczność całego ekranu (1 = pełny, 0 = zniknął)
     * @param barAlpha widoczność paska (gaśnie wcześniej niż logo)
     * @param progress postęp wczytywania 0..1
     */
    public static void render(DrawContext c, float alpha, float barAlpha, float progress) {
        if (alpha <= 0.002f) return;
        int w = c.getScaledWindowWidth(), h = c.getScaledWindowHeight();
        long nowMs = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (nowMs - last) / 1000f);
        last = nowMs;
        float t = seconds();

        // tło – zakrywa standardowy ekran Mojang
        c.fill(0, 0, w, h, argb(alpha, VOID));

        float size = Math.min(w, h) * 0.34f;
        float cx = w / 2f, cy = h / 2f - size * 0.12f;

        // poświata za logo (oddycha)
        float breathe = 0.5f + 0.5f * (float) Math.sin(t * 1.8f);
        float glowSize = size * (2.0f + 0.12f * breathe);
        sprite(c, GLOW, 256, cx - glowSize / 2f, cy - glowSize / 2f, glowSize, glowSize,
                alpha * (0.38f + 0.18f * breathe), true, 0f);

        // logo: delikatne kołysanie i "oddech" skali
        float s = size * (1f + 0.012f * (float) Math.sin(t * 1.6f));
        float rot = (float) Math.sin(t * 0.55f) * 0.045f;
        sprite(c, LOGO, 256, cx - s / 2f, cy - s / 2f, s, s, alpha, false, rot);

        // pasek postępu
        shownProgress = damp(shownProgress, progress, 6f, dt);
        float ba = alpha * barAlpha;
        if (ba > 0.003f) {
            int bw = Math.round(Math.min(220f, w * 0.42f));
            int bx = Math.round(cx - bw / 2f);
            int by = Math.round(cy + size * 0.68f);
            int fillW = Math.round(bw * clamp01(shownProgress));
            c.fill(bx, by, bx + bw, by + 2, argb(ba * 0.14f, 0xFFFFFF));
            // miękka poświata pod wypełnieniem
            c.fill(bx, by - 2, bx + fillW, by + 4, argb(ba * 0.18f, NEON));
            c.fill(bx, by, bx + fillW, by + 2, argb(ba, NEON));
            // jasny błysk przesuwający się po wypełnionej części
            if (fillW > 12) {
                float p = (t * 0.9f) % 1f;
                int sx = bx + Math.round((fillW - 12) * p);
                c.fill(sx, by, sx + 12, by + 2, argb(ba * 0.85f, NEON_HOT));
            }
        }
    }
}
