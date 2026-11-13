package pl.gruzclient.module;

import java.util.Map;

/** Gotowe układy HUD jednym kliknięciem (w edytorze HUD). */
public final class HudPresets {
    private HudPresets() {}

    public static final String[] NAMES = {"pvp", "survival", "minimal", "clean", "streamer"};

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
            case "clean" -> Map.of(
                    Modules.FPS, new Pos(0.01f, 0.01f),
                    Modules.PING, new Pos(0.01f, 0.06f),
                    Modules.CLOCK, new Pos(0.01f, 0.11f));
            // bez koordynatów i pingu: widzowie nie zobaczą, gdzie jesteś
            case "streamer" -> Map.of(
                    Modules.FPS, new Pos(0.01f, 0.01f),
                    Modules.CPS, new Pos(0.01f, 0.06f),
                    Modules.KEYSTROKES, new Pos(0.01f, 0.72f),
                    Modules.ARMOR, new Pos(0.86f, 0.70f),
                    Modules.POTIONS, new Pos(0.86f, 0.30f),
                    Modules.COMBO, new Pos(0.44f, 0.67f));
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

    // ---------------- kod układu do udostępniania ----------------

    /** "GRUZHUD1:" + base64 z "id,x,y,skala;…" (x, y w promilach ekranu, skala x10) – tylko włączone moduły, same liczby */
    public static String encode() {
        StringBuilder sb = new StringBuilder();
        for (HudModule m : Modules.hud()) {
            if (!m.isEnabled()) continue;
            sb.append(m.id).append(',').append(Math.round(m.x * 1000)).append(',').append(Math.round(m.y * 1000))
                    .append(',').append(Math.round(m.scale * 10)).append(';');
        }
        return "GRUZHUD1:" + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Wczytuje układ z kodu od innego gracza. Kod to tylko pozycje: nieznane moduły i złe liczby są pomijane,
     * wartości przycinane do zakresu. false = to nie jest kod układu.
     */
    public static boolean decode(String code) {
        if (code == null || !code.startsWith("GRUZHUD1:") || code.length() > 4096) return false;
        Map<String, float[]> got = new java.util.HashMap<>();
        try {
            String s = new String(java.util.Base64.getUrlDecoder().decode(code.substring(9).trim()), java.nio.charset.StandardCharsets.UTF_8);
            for (String part : s.split(";")) {
                String[] f = part.split(",");
                if (f.length != 4) continue;
                got.put(f[0], new float[]{clamp(Integer.parseInt(f[1]) / 1000f, 0f, 1f), clamp(Integer.parseInt(f[2]) / 1000f, 0f, 1f),
                        clamp(Integer.parseInt(f[3]) / 10f, 0.5f, 2.5f)});
            }
        } catch (IllegalArgumentException e) { // zły base64 albo nie-liczba
            return false;
        }
        boolean any = false;
        for (HudModule m : Modules.hud()) any |= got.containsKey(m.id);
        if (!any) return false; // nie wyłączamy całego HUD przez pusty albo obcy kod
        for (HudModule m : Modules.hud()) {
            float[] p = got.get(m.id);
            m.setEnabled(p != null);
            if (p != null) { m.x = p[0]; m.y = p[1]; m.scale = p[2]; }
        }
        Modules.save();
        return true;
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
