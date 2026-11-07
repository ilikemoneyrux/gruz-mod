package pl.gruzclient.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;

/**
 * Tryb nagrywania: jednym klawiszem chowasz cały HUD i celownik, ale ręka i przedmioty zostają
 * (w odróżnieniu od F1). Idealne do klipów.
 */
public class RecordingModule extends Module {
    public final Setting.Key key = add(new Setting.Key("key", "setting.key", InputConstants.KEY_F6));

    private boolean active, wasDown;

    public RecordingModule() { super("recording", Category.UTILITY, true); }

    @Override
    public void tick() {
        boolean down = Modules.keyDown(key.code);
        if (down && !wasDown) active = !active;
        wasDown = down;
    }

    /** Czy HUD ma być teraz schowany */
    public boolean hidingHud() { return isEnabled() && active; }

    @Override
    protected void onDisable() { active = false; }
}
