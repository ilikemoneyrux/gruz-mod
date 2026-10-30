package pl.gruzclient.screen;

import static pl.gruzclient.render.GruzDraw.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Przycisk w stylu Gruz: ciemna płytka, po najechaniu wysuwa się neonowa belka z lewej,
 * dolna linia rozjeżdża się na całą szerokość, tekst lekko przesuwa się w prawo.
 */
public class GruzButton extends Button {
    private float hover = 0f;
    private long last = 0;
    private float reveal = 1f; // ustawiane przez ekran przy animacji wejścia
    private boolean primary = false;

    public GruzButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public GruzButton primary() { this.primary = true; return this; }
    public void setReveal(float r) { this.reveal = r; }

    @Override
    public void renderWidget(GuiGraphics c, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        hover = damp(hover, this.active && this.isHoveredOrFocused() ? 1f : 0f, 14f, dt);

        float a = this.alpha * reveal;
        if (a <= 0.01f) return;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        int base = primary ? 0x1A0F2A : SLAB;
        c.fill(x, y, x + w, y + h, argb(a * (0.72f + 0.2f * hover), lerpColor(base, 0x22143A, hover)));
        c.renderOutline(x, y, w, h, argb(a * (0.9f), lerpColor(primary ? 0x5A2A9A : SEAM, NEON, hover)));

        // neonowa belka z lewej
        int barH = Math.round((h - 6) * hover);
        if (barH > 0) {
            int by = y + (h - barH) / 2;
            c.fill(x + 1, by, x + 3, by + barH, argb(a, NEON));
        }
        // dolna linia
        int lineW = Math.round((w - 2) * hover);
        if (lineW > 0) c.fill(x + 1, y + h - 2, x + 1 + lineW, y + h - 1, argb(a * 0.85f, NEON));

        Font tr = Minecraft.getInstance().font;
        if (textVisible(a)) {
            int color = this.active ? lerpColor(primary ? NEON_HOT : 0xD6CCE6, 0xFFFFFF, hover) : MUTE;
            int tw = tr.width(getMessage());
            int tx = x + (w - tw) / 2 + Math.round(2 * hover);
            int ty = y + (h - 8) / 2;
            c.drawString(tr, getMessage(), tx, ty, argb(a, color), false);
        }
    }
}
