package pl.gruzclient.legacy;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;

/** HUD Gruz na 1.8.9: lewa kolumna (FPS, CPS, koordynaty), klawisze na dole po lewej, zbroja i mikstury po prawej. */
public final class Hud {
    private Hud() {}

    private static final int NEON = 0xFFB45CFF, BG = 0x8C0C0912, TEXT = 0xFFF1ECF8, MUTE = 0xFF9488A8;

    /** Pastylka z tekstem; zwraca wysokość */
    private static int pill(TextRenderer f, String s, int x, int y) {
        int w = f.getStringWidth(s) + 8;
        DrawableHelper.fill(x, y, x + w, y + 12, BG);
        DrawableHelper.fill(x, y, x + 1, y + 12, NEON);
        f.drawWithShadow(s, x + 4, y + 2, TEXT);
        return 14;
    }

    /** InGameHud.render (koniec) */
    public static void render() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || mc.options.debugEnabled) return;
        Mods.Config c = Mods.cfg;
        TextRenderer f = mc.textRenderer;
        Window win = new Window(mc);
        int sw = win.getWidth(), sh = win.getHeight();
        GlStateManager.enableBlend();

        int y = 4;
        if (c.fps) y += pill(f, MinecraftClient.getCurrentFps() + " FPS", 4, y);
        if (c.cps) y += pill(f, Mods.leftCps() + " | " + Mods.rightCps() + " CPS", 4, y);
        if (c.coords) {
            y += pill(f, String.format("X %.0f  Y %.0f  Z %.0f", mc.player.x, mc.player.y, mc.player.z), 4, y);
        }

        if (c.keystrokes) keystrokes(mc, f, 4, sh - 78);
        int ry = 4;
        if (c.armor) ry = armor(mc, f, sw, ry);
        if (c.potions) potions(mc, f, sw, ry + 4);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void key(TextRenderer f, KeyBinding k, String label, int x, int y, int w, int h) {
        boolean on = k.isPressed();
        DrawableHelper.fill(x, y, x + w, y + h, on ? 0xCCB45CFF : BG);
        f.drawWithShadow(label, x + (w - f.getStringWidth(label)) / 2f, y + (h - 8) / 2f, on ? 0xFF12051F : TEXT);
    }

    private static void keystrokes(MinecraftClient mc, TextRenderer f, int x, int y) {
        int s = 20, g = 2;
        key(f, mc.options.forwardKey, "W", x + s + g, y, s, s);
        key(f, mc.options.leftKey, "A", x, y + s + g, s, s);
        key(f, mc.options.backKey, "S", x + s + g, y + s + g, s, s);
        key(f, mc.options.rightKey, "D", x + 2 * (s + g), y + s + g, s, s);
        int w = (3 * s + 2 * g - g) / 2, yy = y + 2 * (s + g);
        key(f, mc.options.attackKey, "LMB " + Mods.leftCps(), x, yy, w, 14);
        key(f, mc.options.useKey, "RMB " + Mods.rightCps(), x + w + g, yy, w, 14);
        key(f, mc.options.jumpKey, "—", x, yy + 16, 3 * s + 2 * g, 10);
    }

    /** Zbroja od hełmu w dół, z wytrzymałością; zwraca y pod spodem */
    private static int armor(MinecraftClient mc, TextRenderer f, int sw, int y) {
        ItemStack[] stacks = mc.player.getArmorStacks();
        DiffuseLighting.enable();
        for (int i = 3; i >= 0; i--) {
            ItemStack st = stacks[i];
            if (st == null) continue;
            int x = sw - 22;
            mc.getItemRenderer().renderInGuiWithOverrides(st, x, y);
            if (st.isDamageable()) {
                String d = String.valueOf(st.getMaxDamage() - st.getDamage());
                DiffuseLighting.disable();
                f.drawWithShadow(d, x - 2 - f.getStringWidth(d), y + 5, TEXT);
                DiffuseLighting.enable();
            }
            y += 18;
        }
        DiffuseLighting.disable();
        return y;
    }

    private static void potions(MinecraftClient mc, TextRenderer f, int sw, int y) {
        for (StatusEffectInstance e : mc.player.getStatusEffectInstances()) {
            String name = I18n.translate(e.getTranslationKey());
            if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
            int sec = e.getDuration() / 20;
            String t = String.format("%d:%02d", sec / 60, sec % 60);
            String s = name + "  " + t;
            int w = f.getStringWidth(s) + 8, x = sw - w - 4;
            DrawableHelper.fill(x, y, x + w, y + 12, BG);
            DrawableHelper.fill(x + w - 1, y, x + w, y + 12, NEON);
            f.drawWithShadow(name, x + 4, y + 2, TEXT);
            f.drawWithShadow(t, x + 4 + f.getStringWidth(name + "  "), y + 2, MUTE);
            y += 14;
        }
    }
}
