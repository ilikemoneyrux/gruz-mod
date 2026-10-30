package pl.gruzclient.module.impl;

import pl.gruzclient.module.Module;

/** Hitboxy encji (to samo co F3+B, ale zapamiętane między uruchomieniami gry). */
public class HitboxesModule extends Module {
    private boolean applied;

    public HitboxesModule() { super("hitboxes", Category.VISUAL, false); }

    @Override
    public void tick() {
        // ustawiamy raz po wejściu do świata; F3+B dalej działa normalnie
        if (!applied && mc.world != null) {
            mc.getEntityRenderDispatcher().setRenderHitboxes(true);
            applied = true;
        }
        if (mc.world == null) applied = false;
    }

    @Override
    protected void onEnable() { applied = false; }

    @Override
    protected void onDisable() {
        applied = false;
        if (mc.world != null) mc.getEntityRenderDispatcher().setRenderHitboxes(false);
    }
}
