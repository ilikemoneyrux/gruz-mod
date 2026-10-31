package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import pl.gruzclient.module.HudModule;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffectInstance;

import static pl.gruzclient.render.GruzDraw.*;

/** Aktywne efekty mikstur: ikona, nazwa z poziomem i pozostały czas. */
public class PotionHudModule extends HudModule {
    public PotionHudModule() { super("potions", true, 0.88f, 0.42f); }

    @Override
    protected int[] draw(GuiGraphics c, boolean editing) {
        var tr = mc.font;
        List<MobEffectInstance> effects = mc.player != null ? new ArrayList<>(mc.player.getActiveEffects()) : List.of();
        if (effects.isEmpty()) {
            if (!editing) return new int[]{0, 0};
            String a = "Speed II", b = "1:30";
            int w = tr.width(a) + 34;
            panel(c, w, 24);
            c.drawString(tr, a, 26, 4, textColor(), !background.value);
            c.drawString(tr, b, 26, 14, argb(1f, MUTE), !background.value);
            return new int[]{w, 24};
        }
        int w = 0;
        for (MobEffectInstance e : effects) w = Math.max(w, tr.width(name(e)) + 34);
        int h = effects.size() * 24;
        panel(c, w, h);
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance e = effects.get(i);
            int y = i * 24;
            Compat.effectIcon(c, e, 5, y + 3, 18);
            c.drawString(tr, name(e), 26, y + 4, textColor(), !background.value);
            boolean blink = !e.isInfiniteDuration() && e.getDuration() < 200 && (System.currentTimeMillis() / 400) % 2 == 0;
            c.drawString(tr, time(e), 26, y + 14, argb(blink ? 0.4f : 1f, MUTE), !background.value);
        }
        return new int[]{w, h};
    }

    private static String name(MobEffectInstance e) {
        String n = Compat.effectName(e);
        int lvl = e.getAmplifier() + 1;
        return lvl > 1 ? n + " " + roman(lvl) : n;
    }

    private static String time(MobEffectInstance e) {
        if (e.isInfiniteDuration()) return "∞";
        int s = e.getDuration() / 20;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n < r.length ? r[n] : String.valueOf(n);
    }
}
