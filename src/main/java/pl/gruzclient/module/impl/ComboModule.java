package pl.gruzclient.module.impl;

import pl.gruzclient.combat.Combat;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.HudModule;
import pl.gruzclient.module.Setting;

import static pl.gruzclient.render.GruzDraw.*;

import net.minecraft.client.gui.GuiGraphics;

/** Combo (trafienia z rzędu bez otrzymania obrażeń) i kille w tej sesji. */
public class ComboModule extends HudModule {
    public final Setting.Bool showCombo = add(new Setting.Bool("combo", "setting.showCombo", true));
    public final Setting.Bool showKills = add(new Setting.Bool("kills", "setting.showKills", true));

    private float pulse = 0f;
    private int lastCombo = 0;
    private long last = 0;

    public ComboModule() { super("combo", false, 0.01f, 0.25f); }

    @Override
    protected int[] draw(GuiGraphics c, boolean editing) {
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        if (Combat.combo > lastCombo) pulse = 1f;
        lastCombo = Combat.combo;
        pulse = damp(pulse, 0f, 8f, dt);

        var tr = mc.font;
        int combo = editing && Combat.combo == 0 ? 4 : Combat.combo;
        String a = showCombo.value ? Lang.t("hud.combo") + " " + combo : "";
        String b = showKills.value ? Lang.t("hud.kills") + " " + Combat.kills : "";
        int w = Math.max(tr.width(a), tr.width(b)) + 10;
        int h = (a.isEmpty() || b.isEmpty() ? 1 : 2) * 10 + 6;
        panel(c, w, h);
        int y = 4;
        if (!a.isEmpty()) {
            int col = combo >= 3 ? lerpColor(color.rgb(), NEON_HOT, Math.min(1f, combo / 8f)) : color.rgb();
            col = lerpColor(col, 0xFFFFFF, pulse * 0.6f);
            c.drawString(tr, a, 5, y, 0xFF000000 | col, !background.value);
            y += 10;
        }
        if (!b.isEmpty()) c.drawString(tr, b, 5, y, textColor(), !background.value);
        return new int[]{w, h};
    }
}
