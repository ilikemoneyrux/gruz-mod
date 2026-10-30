package pl.gruzclient.module.impl;

import pl.gruzclient.combat.Combat;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.TextHudModule;

/** Odległość, z jakiej trafiłeś ostatni cios (w blokach). */
public class ReachModule extends TextHudModule {
    public ReachModule() { super("reach", false, 0.01f, 0.20f); }

    @Override
    protected String value(boolean editing) {
        if (Combat.lastReach < 0 || System.currentTimeMillis() - Combat.lastReachAt > 5000) return editing ? "3.02" : "–";
        return String.format("%.2f", Combat.lastReach);
    }

    @Override protected String label() { return Lang.t("hud.blocks"); }
}
