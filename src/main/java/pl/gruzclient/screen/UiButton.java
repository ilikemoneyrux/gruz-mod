package pl.gruzclient.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import pl.gruzclient.render.Ui;

import static pl.gruzclient.render.GruzDraw.*;

/**
 * Przycisk w nowym stylu Gruz: zaokrąglony ciemny panel z ikoną i tekstem.
 * Warianty: zwykły, akcentowy (gradient w kolorze motywu), "niebezpieczny" (wyjście) i sama ikona.
 */
public class UiButton extends ButtonWidget {
    public enum Kind { NORMAL, ACCENT, DANGER, ICON }

    private final Ui.Icon icon;
    private Kind kind = Kind.NORMAL;
    private String label;
    private String badge;
    private float hover = 0f, reveal = 1f;
    private long last = 0;
    private boolean bigText = false;

    public UiButton(int x, int y, int w, int h, Ui.Icon icon, String label, PressAction onPress) {
        super(x, y, w, h, Text.literal(label == null ? "" : label), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.icon = icon;
        this.label = label;
    }

    public UiButton kind(Kind k) { this.kind = k; return this; }
    public UiButton big() { this.bigText = true; return this; }
    public UiButton badge(String b) { this.badge = b; return this; }
    public void setReveal(float r) { this.reveal = r; }
    public void setLabel(String l) { this.label = l; setMessage(Text.literal(l)); }

    @Override
    protected void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        hover = damp(hover, active && isSelected() ? 1f : 0f, 16f, dt);
        float a = alpha * reveal;
        if (a < 0.01f) return;
        int x = getX(), y = getY() - Math.round(hover), w = getWidth(), h = getHeight();

        switch (kind) {
            case ACCENT -> {
                Ui.hGradient(c, x, y, w, h, lerpColor(NEON, NEON_HOT, 0.15f * hover), lerpColor(NEON_DEEP, 0x1A0F2A, 0.4f), a, 4);
                Ui.roundBorder(c, x, y, w, h, 4, argb(a * (0.5f + 0.5f * hover), NEON_HOT));
            }
            case DANGER -> {
                Ui.round(c, x, y, w, h, 4, argb(a * (0.55f + 0.25f * hover), lerpColor(0x2A0C16, 0x4A1426, hover)));
                Ui.roundBorder(c, x, y, w, h, 4, argb(a * (0.35f + 0.5f * hover), 0xFF5C8A));
            }
            default -> {
                Ui.round(c, x, y, w, h, 4, argb(a * (0.78f + 0.12f * hover), lerpColor(0x0A0810, 0x1A1226, hover)));
                Ui.roundBorder(c, x, y, w, h, 4, argb(a * (0.1f + 0.75f * hover), lerpColor(0xFFFFFF, NEON, hover)));
            }
        }

        int textCol = kind == Kind.DANGER ? lerpColor(0xFF8FAA, 0xFFFFFF, hover)
                : kind == Kind.ACCENT ? 0xFFFFFF : lerpColor(0xE6E0F0, 0xFFFFFF, hover);
        int iconCol = kind == Kind.NORMAL || kind == Kind.ICON ? lerpColor(0xD6CCE6, NEON_HOT, hover) : textCol;

        if (kind == Kind.ICON || label == null || label.isEmpty()) {
            int s = Math.min(w, h) - 10;
            if (icon != null) Ui.icon(c, icon, x + (w - s) / 2, y + (h - s) / 2, s, argb(a, iconCol));
        } else {
            Text t = bigText ? Ui.bold(label) : Ui.text(label);
            int tw = Ui.width(t);
            int is = icon != null ? Math.min(12, h - 10) : 0, gap = icon != null ? 6 : 0;
            int total = tw + is + gap;
            int sx = x + (w - total) / 2;
            if (icon != null) Ui.icon(c, icon, sx, y + (h - is) / 2, is, argb(a, iconCol));
            c.drawText(Ui.tr(), t, sx + is + gap, y + (h - 8) / 2, argb(a, textCol), false);
        }
        if (badge != null && !badge.isEmpty()) {
            Text b = Ui.bold(badge);
            int bw = Ui.width(b) + 6;
            Ui.round(c, x + w - bw + 3, y - 4, bw, 10, 3, argb(a, NEON));
            c.drawText(Ui.tr(), b, x + w - bw + 6, y - 3, argb(a, 0xFFFFFF), false);
        }
    }
}
