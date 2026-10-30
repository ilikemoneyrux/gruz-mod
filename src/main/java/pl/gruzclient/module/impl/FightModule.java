package pl.gruzclient.module.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import pl.gruzclient.combat.Combat;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.HudModule;
import pl.gruzclient.module.Setting;

import static pl.gruzclient.render.GruzDraw.*;

/**
 * Statystyki walki: czas, ciosy zadane / otrzymane, najlepsze combo i obrażenia.
 * Widać je w trakcie walki i chwilę po niej, a po walce w czacie pojawia się podsumowanie.
 */
public class FightModule extends HudModule {
    public final Setting.Bool summary = add(new Setting.Bool("summary", "setting.fightSummary", true));
    private String lastLine = "";
    private long lastShownUntil = 0;

    public FightModule() { super("fight", false, 0.01f, 0.5f); }

    public void onFightEnd(boolean died, long millis) {
        if (!isEnabled() || millis < 1500 || Combat.fightDealt + Combat.fightTaken < 3) return;
        lastLine = Lang.t(died ? "fight.lost" : "fight.end")
                .replace("{opp}", Combat.fightOpponent)
                .replace("{time}", String.format("%.1f", millis / 1000f))
                .replace("{dealt}", String.valueOf(Combat.fightDealt))
                .replace("{taken}", String.valueOf(Combat.fightTaken))
                .replace("{combo}", String.valueOf(Combat.fightBestCombo))
                .replace("{dmg}", String.format("%.1f", Combat.fightDamageTaken / 2f));
        lastShownUntil = System.currentTimeMillis() + 5000;
        if (summary.value && mc.player != null) mc.player.sendMessage(Text.literal("§d[Gruz] §f" + lastLine), false);
    }

    @Override
    protected int[] draw(DrawContext c, boolean editing) {
        boolean live = Combat.inFight();
        if (!live && !editing && System.currentTimeMillis() > lastShownUntil) return new int[]{lastW, lastH};
        var tr = mc.textRenderer;
        String head = Lang.t("fight.title") + (live ? "  " + String.format("%.1fs", Combat.fightMillis() / 1000f) : "");
        String[] lines = {
                Lang.t("fight.dealt") + "  " + (editing && !live ? 7 : Combat.fightDealt),
                Lang.t("fight.taken") + "  " + (editing && !live ? 3 : Combat.fightTaken),
                Lang.t("fight.combo") + "  " + (editing && !live ? 4 : Combat.fightBestCombo),
        };
        int w = tr.getWidth(head);
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        w += 12;
        int h = 16 + lines.length * 10;
        panel(c, w, h);
        c.drawText(tr, head, 6, 4, argb(1f, NEON_HOT), !background.value);
        for (int i = 0; i < lines.length; i++) c.drawText(tr, lines[i], 6, 15 + i * 10, textColor(), !background.value);
        return new int[]{w, h};
    }
}
