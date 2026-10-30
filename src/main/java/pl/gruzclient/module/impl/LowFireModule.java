package pl.gruzclient.module.impl;

import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/** Mniejszy ogień na ekranie i niżej trzymana tarcza – więcej widać w trakcie walki. */
public class LowFireModule extends Module {
    public final Setting.Number fire = add(new Setting.Number("fire", "setting.fireHeight", 30f, 0f, 45f, 5f, "%"));
    public final Setting.Number shield = add(new Setting.Number("shield", "setting.shieldHeight", 20f, 0f, 45f, 5f, "%"));

    public LowFireModule() { super("lowfire", Category.VISUAL, true); }

    /** Przesunięcie w dół (w jednostkach ekranu 0..0.45) */
    public float fireOffset() { return isEnabled() ? fire.value / 100f : 0f; }
    public float shieldOffset() { return isEnabled() ? shield.value / 100f : 0f; }
}
