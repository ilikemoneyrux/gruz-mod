package pl.gruzclient.render;

import pl.gruzclient.compat.Compat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import pl.gruzclient.GruzClient;

import static pl.gruzclient.render.GruzDraw.*;

/**
 * Wspólne klocki nowego wyglądu Gruz (styl "nowoczesny launcher"): czcionka Poppins, ikony,
 * zaokrąglone półprzezroczyste panele, przyciski z ikonami i akcentowy gradient.
 */
public final class Ui {
    private Ui() {}

    public static final ResourceLocation FONT = Compat.id(GruzClient.MOD_ID, "ui");
    public static final ResourceLocation FONT_BOLD = Compat.id(GruzClient.MOD_ID, "ui_bold");
    public static final ResourceLocation ICONS = Compat.id(GruzClient.MOD_ID, "textures/gui/icons.png");

    /** Ikony w atlasie icons.png (8 x 4, po 32 px) */
    public enum Icon {
        USER, USERS, STAR, SHIRT, GEAR, GRID, CHAT, CAMERA,
        GLOBE, POWER, MOVE, MINUS, PLUS, SMILE, LAYOUT, MAGNET,
        SEARCH, CLOSE, BACK, PENCIL, MAP, TROPHY, EYE, CHECK
    }

    // ---------------- tekst ----------------
    public static MutableComponent text(String s) { return Component.literal(s).setStyle(Style.EMPTY.withFont(FONT)); }
    public static MutableComponent bold(String s) { return Component.literal(s).setStyle(Style.EMPTY.withFont(FONT_BOLD)); }

    public static Font tr() { return Minecraft.getInstance().font; }
    public static int width(Component t) { return tr().width(t); }

    public static void draw(GuiGraphics c, Component t, int x, int y, int argb) { c.drawString(tr(), t, x, y, argb, false); }
    public static void drawCentered(GuiGraphics c, Component t, int cx, int y, int argb) { c.drawString(tr(), t, cx - width(t) / 2, y, argb, false); }

    /** Tekst w skali (np. duże tytuły) */
    public static void drawScaled(GuiGraphics c, Component t, float x, float y, float scale, int argb, boolean centered) {
        var m = c.pose();
        m.pushPose();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        c.drawString(tr(), t, centered ? -width(t) / 2 : 0, 0, argb, false);
        m.popPose();
    }

    /** Ucina tekst do szerokości z "…" */
    public static MutableComponent fit(String s, int maxW, boolean b) {
        MutableComponent t = b ? bold(s) : text(s);
        if (width(t) <= maxW) return t;
        String cut = s;
        while (cut.length() > 1 && width(b ? bold(cut + "…") : text(cut + "…")) > maxW) cut = cut.substring(0, cut.length() - 1);
        return b ? bold(cut + "…") : text(cut + "…");
    }

    // ---------------- ikony ----------------
    public static void icon(GuiGraphics c, Icon icon, int x, int y, int size, int argb) {
        GruzDraw.tex(ICONS);
        int i = icon.ordinal();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Compat.blit(c, ICONS, x, y, size, size, (i % 8) * 32f, (i / 8) * 32f, 32, 32, 256, 128, argb);
    }

    // ---------------- kształty ----------------
    /** Prostokąt z zaokrąglonymi rogami (promień w pikselach GUI) */
    public static void round(GuiGraphics c, int x, int y, int w, int h, int r, int argb) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { c.fill(x, y, x + w, y + h, argb); return; }
        c.fill(x, y + r, x + w, y + h - r, argb);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, argb);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, argb);
        }
    }

    /** Obrys zaokrąglonego prostokąta (1 px) */
    public static void roundBorder(GuiGraphics c, int x, int y, int w, int h, int r, int argb) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        int[] ins = new int[Math.max(1, r)];
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            ins[i] = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
        }
        int top = r > 0 ? ins[0] : 0;
        c.fill(x + top, y, x + w - top, y + 1, argb);
        c.fill(x + top, y + h - 1, x + w - top, y + h, argb);
        for (int i = 1; i < r; i++) {
            int a = ins[i], b = Math.max(ins[i - 1], a + 1);
            c.fill(x + a, y + i, x + b, y + i + 1, argb);
            c.fill(x + w - b, y + i, x + w - a, y + i + 1, argb);
            c.fill(x + a, y + h - 1 - i, x + b, y + h - i, argb);
            c.fill(x + w - b, y + h - 1 - i, x + w - a, y + h - i, argb);
        }
        c.fill(x, y + r, x + 1, y + h - r, argb);
        c.fill(x + w - 1, y + r, x + w, y + h - r, argb);
    }

    /** Ciemny półprzezroczysty panel jak w nowoczesnych klientach */
    public static void panel(GuiGraphics c, int x, int y, int w, int h, float alpha) {
        round(c, x, y, w, h, 4, argb(0.78f * alpha, 0x0A0810));
        roundBorder(c, x, y, w, h, 4, argb(0.08f * alpha, 0xFFFFFF));
    }

    /** Poziomy gradient (rysowany kolumnami) – akcentowe przyciski */
    public static void hGradient(GuiGraphics c, int x, int y, int w, int h, int left, int right, float alpha, int r) {
        for (int i = 0; i < w; i++) {
            int col = argb(alpha, GruzDraw.lerpColor(left, right, i / (float) Math.max(1, w - 1)));
            int inset = 0;
            if (i < r || i >= w - r) {
                int k = i < r ? i : w - 1 - i;
                double dx = r - k - 0.5;
                inset = (int) Math.round(r - Math.sqrt(r * r - dx * dx));
            }
            c.fill(x + i, y + inset, x + i + 1, y + h - inset, col);
        }
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Wygaszony kolor akcentu (np. tło przycisku akcentowego) */
    public static int accentDark() { return GruzDraw.mix(NEON, 0x000000, 0.55f); }
}
