package pl.gruzclient.module;

import java.util.Map;

/** Gotowe układy HUD jednym kliknięciem (w edytorze HUD). */
public final class HudPresets {
    private HudPresets() {}

    public static final String[] NAMES = {"pvp", "survival", "minimal"};

    private record Pos(float x, float y) {}

    private static Map<HudModule, Pos> layout(String name) {
        return switch (name) {
            case "pvp" -> Map.of(
                    Modules.FPS, new Pos(0.01f, 0.01f),
                    Modules.CPS, new Pos(0.01f, 0.06f),
                    Modules.PING, new Pos(0.01f, 0.11f),
                    Modules.KEYSTROKES, new Pos(0.01f, 0.72f),
                    Modules.ARMOR, new Pos(0.86f, 0.70f),
                    Modules.POTIONS, new Pos(0.86f, 0.30f),
                    Modules.TARGET_HUD, new Pos(0.56f, 0.56f),
                    Modules.REACH, new Pos(0.44f, 0.62f),
                    Modules.COMBO, new Pos(0.44f, 0.67f),
                    Modules.FIGHT, new Pos(0.01f, 0.40f));
            case "survival" -> Map.of(
                    Modules.FPS, new Pos(0.01f, 0.01f),
                    Modules.COORDS, new Pos(0.01f, 0.06f),
                    Modules.CLOCK, new Pos(0.01f, 0.22f),
                    Modules.ARMOR, new Pos(0.01f, 0.70f),
                    Modules.POTIONS, new Pos(0.86f, 0.40f),
                    Modules.MINIMAP, new Pos(0.84f, 0.03f),
                    Modules.SPEED, new Pos(0.01f, 0.28f));
            default -> Map.of(
                    Modules.FPS, new Pos(0.01f, 0.01f));
        };
    }

    public static void apply(String name) {
        Map<HudModule, Pos> l = layout(name);
        for (HudModule m : Modules.hud()) {
            Pos p = l.get(m);
            m.setEnabled(p != null);
            if (p != null) { m.x = p.x(); m.y = p.y(); m.scale = 1f; }
        }
        if ("minimal".equals(name)) Modules.MINIMAP.compass.value = false;
        Modules.save();
    }
}
