package pl.gruzclient.module.impl;

import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/** Pora dnia, pogoda i "kosmiczne niebo" – zmiany widzisz tylko Ty. */
public class WorldModule extends Module {
    public final Setting.Choice time = add(new Setting.Choice("time", "setting.time", 0,
            "time.server", "time.morning", "time.noon", "time.sunset", "time.night", "time.midnight"));
    public final Setting.Bool clearWeather = add(new Setting.Bool("clear", "setting.clearWeather", true));
    public final Setting.Bool cosmicSky = add(new Setting.Bool("cosmic", "setting.cosmicSky", false));

    private static final long[] TIMES = {-1, 1000, 6000, 12500, 15000, 18000};

    public WorldModule() { super("world", Category.VISUAL, false); }

    /** Stała pora dnia albo -1, gdy zostaje serwerowa */
    public long fixedTime() { return isEnabled() ? TIMES[time.index] : -1; }
    public boolean noRain() { return isEnabled() && clearWeather.value; }
    public boolean cosmic() { return isEnabled() && cosmicSky.value; }
}
