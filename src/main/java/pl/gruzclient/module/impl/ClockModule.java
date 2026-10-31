package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Setting;
import pl.gruzclient.module.TextHudModule;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Zegar: godzina na komputerze, godzina i dzień w grze albo czas tej sesji. */
public class ClockModule extends TextHudModule {
    public final Setting.Choice mode = add(new Setting.Choice("mode", "setting.clockMode", 0,
            "clock.real", "clock.game", "clock.session"));
    public final Setting.Bool h12 = add(new Setting.Bool("h12", "setting.clock12", false));
    private final long started = System.currentTimeMillis();

    public ClockModule() { super("clock", false, 0.01f, 0.42f); }

    @Override
    protected String value(boolean editing) {
        switch (mode.index) {
            case 1 -> {
                if (mc.level == null) return "06:00";
                long t = Compat.dayTime(mc.level);
                long day = t / 24000 + 1;
                long ticks = (t + 6000) % 24000; // 0 ticków = 6:00
                int hh = (int) (ticks / 1000), mm = (int) ((ticks % 1000) * 60 / 1000);
                return fmt(hh, mm) + "  ·  " + Lang.t("clock.day") + " " + day;
            }
            case 2 -> {
                long s = (System.currentTimeMillis() - started) / 1000;
                return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format("%d:%02d", s / 60, s % 60);
            }
            default -> {
                return LocalTime.now().format(DateTimeFormatter.ofPattern(h12.value ? "h:mm a" : "HH:mm"));
            }
        }
    }

    private String fmt(int hh, int mm) {
        if (!h12.value) return String.format("%02d:%02d", hh, mm);
        return String.format("%d:%02d %s", hh % 12 == 0 ? 12 : hh % 12, mm, hh < 12 ? "AM" : "PM");
    }

    @Override
    protected String label() { return mode.index == 2 ? Lang.t("clock.sessionShort") : ""; }
}
