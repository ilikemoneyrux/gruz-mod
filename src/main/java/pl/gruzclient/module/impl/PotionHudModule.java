package pl.gruzclient.module.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffectInstance;
import pl.gruzclient.module.HudModule;

import java.util.ArrayList;
import java.util.List;

import static pl.gruzclient.render.GruzDraw.*;

/** Aktywne efekty mikstur: ikona, nazwa z poziomem i pozostały czas. */
public class PotionHudModule extends HudModule {
    public PotionHudModule() { super("potions", true, 0.88f, 0.42f); }

    @Override
    protected int[] draw(DrawContext c, boolean editing) {
        var tr = mc.textRenderer;
        List<StatusEffectInstance> effects = mc.player != null ? new ArrayList<>(mc.player.getStatusEffects()) : List.of();
        if (effects.isEmpty()) {
            if (!editing) return new int[]{0, 0};
            String a = "Speed II", b = "1:30";
            int w = tr.getWidth(a) + 34;
            panel(c, w, 24);
            c.drawText(tr, a, 26, 4, textColor(), !background.value);
            c.drawText(tr, b, 26, 14, argb(1f, MUTE), !background.value);
            return new int[]{w, 24};
        }
        int w = 0;
        for (StatusEffectInstance e : effects) w = Math.max(w, tr.getWidth(name(e)) + 34);
        int h = effects.size() * 24;
        panel(c, w, h);
        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance e = effects.get(i);
            int y = i * 24;
            Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(e.getEffectType());
            c.drawSprite(5, y + 3, 0, 18, 18, sprite);
            c.drawText(tr, name(e), 26, y + 4, textColor(), !background.value);
            boolean blink = !e.isInfinite() && e.getDuration() < 200 && (System.currentTimeMillis() / 400) % 2 == 0;
            c.drawText(tr, time(e), 26, y + 14, argb(blink ? 0.4f : 1f, MUTE), !background.value);
        }
        return new int[]{w, h};
    }

    private static String name(StatusEffectInstance e) {
        String n = e.getEffectType().value().getName().getString();
        int lvl = e.getAmplifier() + 1;
        return lvl > 1 ? n + " " + roman(lvl) : n;
    }

    private static String time(StatusEffectInstance e) {
        if (e.isInfinite()) return "∞";
        int s = e.getDuration() / 20;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n < r.length ? r[n] : String.valueOf(n);
    }
}
