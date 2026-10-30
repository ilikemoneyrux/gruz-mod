package pl.gruzclient.module.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import pl.gruzclient.combat.Combat;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.HudModule;

import static pl.gruzclient.render.GruzDraw.*;

/** Karta przeciwnika: głowa, nick, życie (płynny pasek), zbroja i odległość. */
public class TargetHudModule extends HudModule {
    private float shownHealth = -1f;
    private float fade = 0f;
    private long last = 0;
    private LivingEntity lastTarget;
    private LivingEntity shownFor;

    public TargetHudModule() { super("targethud", true, 0.42f, 0.62f); }

    @Override
    protected int[] draw(DrawContext c, boolean editing) {
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        LivingEntity t = Combat.target;
        if (t != null) lastTarget = t;
        fade = damp(fade, t != null || editing ? 1f : 0f, 10f, dt);
        int w = 150, h = 40;
        LivingEntity e = t != null ? t : lastTarget;
        if (fade < 0.02f || (e == null && !editing)) return new int[]{w, h};

        var tr = mc.textRenderer;
        String name = e != null ? e.getDisplayName().getString() : Lang.t("hud.enemy");
        float hp = e != null ? e.getHealth() + e.getAbsorptionAmount() : 14f;
        float max = e != null ? Math.max(1f, e.getMaxHealth()) : 20f;
        int armor = e != null ? e.getArmor() : 12;
        if (shownHealth < 0 || e != shownFor) { shownHealth = hp; shownFor = e; }
        shownHealth = damp(shownHealth, hp, 9f, dt);

        c.fill(0, 0, w, h, argb(0.7f * fade, SLAB));
        c.fill(0, 0, 2, h, argb(fade, NEON));
        // głowa gracza (dla mobów – kwadrat w kolorze motywu)
        if (e instanceof PlayerEntity p && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (entry != null) {
                c.setShaderColor(1f, 1f, 1f, fade);
                PlayerSkinDrawer.draw(c, entry.getSkinTextures(), 6, 6, 28);
                c.setShaderColor(1f, 1f, 1f, 1f);
            } else c.fill(6, 6, 34, 34, argb(fade, SEAM));
        } else c.fill(6, 6, 34, 34, argb(fade * 0.8f, NEON_DEEP));

        if (textVisible(fade)) {
            c.drawText(tr, tr.trimToWidth(name, w - 46), 40, 6, argb(fade, TEXT), false);
            String hpText = String.format("%.1f", hp / 2f) + " ❤";
            c.drawText(tr, hpText, 40, 17, argb(fade, hp / max < 0.3f ? 0xFF5C8A : MUTE), false);
            String ar = armor + " " + Lang.t("hud.armor");
            c.drawText(tr, ar, w - 6 - tr.getWidth(ar), 17, argb(fade, MUTE), false);
        }
        // pasek życia: tło, "opóźniony" biały ślad, wypełnienie w kolorze motywu
        int bx = 40, by = 30, bw = w - 46;
        float frac = clamp01(hp / max), shown = clamp01(shownHealth / max);
        c.fill(bx, by, bx + bw, by + 4, argb(fade * 0.6f, SEAM));
        c.fill(bx, by, bx + Math.round(bw * Math.max(frac, shown)), by + 4, argb(fade * 0.8f, 0xFFFFFF));
        int col = frac < 0.3f ? 0xFF5C8A : frac < 0.6f ? 0xFFD35C : NEON;
        c.fill(bx, by, bx + Math.round(bw * Math.min(frac, shown)), by + 4, argb(fade, col));
        return new int[]{w, h};
    }
}
