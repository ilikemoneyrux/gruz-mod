package pl.gruzclient.module.impl;

import pl.gruzclient.module.Setting;
import pl.gruzclient.module.TextHudModule;

/** Prędkość w blokach na sekundę (poziomo, opcjonalnie z ruchem w pionie). */
public class SpeedModule extends TextHudModule {
    public final Setting.Bool vertical = add(new Setting.Bool("vertical", "setting.speedVertical", false));
    private double prevX = Double.NaN, prevY, prevZ;
    private double speed;

    public SpeedModule() { super("speed", false, 0.01f, 0.36f); }

    @Override
    public void tick() {
        if (mc.player == null) { prevX = Double.NaN; return; }
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        if (!Double.isNaN(prevX)) {
            double dx = x - prevX, dy = vertical.value ? y - prevY : 0, dz = z - prevZ;
            double now = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20;
            if (now > 100) now = 0; // teleport
            speed += (now - speed) * 0.35; // wygładzenie
        }
        prevX = x; prevY = y; prevZ = z;
    }

    @Override
    protected String value(boolean editing) { return String.format("%.2f", editing && mc.player == null ? 5.61 : speed); }

    @Override
    protected String label() { return "m/s"; }
}
