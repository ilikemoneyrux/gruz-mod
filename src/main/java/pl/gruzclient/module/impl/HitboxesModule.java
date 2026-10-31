package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import pl.gruzclient.module.Module;

/** Hitboxy encji (to samo co F3+B, ale zapamiętane między uruchomieniami gry). */
public class HitboxesModule extends Module {
    private boolean applied;

    public HitboxesModule() { super("hitboxes", Category.VISUAL, false); }

    @Override
    public void tick() {
        // ustawiamy raz po wejściu do świata; F3+B dalej działa normalnie
        if (!applied && mc.level != null) {
            Compat.hitboxes(mc, true);
            applied = true;
        }
        if (mc.level == null) applied = false;
    }

    @Override
    protected void onEnable() { applied = false; }

    @Override
    protected void onDisable() {
        applied = false;
        if (mc.level != null) Compat.hitboxes(mc, false);
    }
}
