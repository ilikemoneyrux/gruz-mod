package pl.gruzclient.cosmetics;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.StringDecomposer;
import pl.gruzclient.render.GruzDraw;

/** Animowane nicki (kosmetyka "name"): tęcza, neon, złoto, ogień, lód, pustka. Widzą je inni gracze Gruz. */
public final class NameEffects {
    private NameEffects() {}

    /**
     * Koloruje tylko sam nick (np. w "[VIP] Gracz" ranga zostaje w kolorach serwera).
     * @param nick nazwa konta; gdy nie ma jej w tekście, kolorujemy cały tekst
     */
    public static Component apply(Component original, CosmeticSet set, String nick) {
        if (set == null || !set.hasName()) return original;
        // znak po znaku z gotowym stylem; stare kody § (nicki z wielu serwerów) rozwijamy tutaj,
        // bo po rozbiciu na pojedyncze znaki „§” i cyfra koloru pokazałyby się jako zwykły tekst
        java.util.List<Style> styles = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        original.visit((style, segment) -> {
            StringDecomposer.iterateFormatted(segment, style, (i, st, cp) -> {
                sb.appendCodePoint(cp);
                styles.add(st);
                return true;
            });
            return java.util.Optional.empty();
        }, Style.EMPTY);
        String full = sb.toString();
        if (full.isEmpty()) return original;
        int start = nick == null ? -1 : full.indexOf(nick);
        int from = start >= 0 ? start : 0, to = start >= 0 ? start + nick.length() : full.length();
        float t = (System.currentTimeMillis() % 100_000L) / 1000f;
        MutableComponent out = Component.empty();
        for (int g = 0, k = 0; g < full.length(); k++) {
            int cp = full.codePointAt(g);
            String ch = new String(Character.toChars(cp));
            Style style = styles.get(k);
            if (g >= from && g < to) style = style.withColor(TextColor.fromRgb(color(set.name(), set.color(), g - from, to - from, t)));
            out.append(Component.literal(ch).setStyle(style));
            g += Character.charCount(cp);
        }
        return out;
    }

    private static int color(String effect, int accent, int i, int n, float t) {
        return switch (effect) {
            case "rainbow" -> hsv((i * 0.06f + t * 0.35f) % 1f, 0.75f, 1f);
            case "neon" -> GruzDraw.mix(accent, 0xFFFFFF, wave(t * 2.2f - i * 0.35f) * 0.6f);
            case "gold" -> GruzDraw.mix(0xE8A317, 0xFFF4C2, band(i, n, t));
            case "fire" -> {
                float f = wave(t * 7f + i * 1.3f) * 0.6f + wave(t * 11f - i * 0.7f) * 0.4f;
                yield f < 0.5f ? GruzDraw.mix(0xC81E0A, 0xFF7A1A, f * 2f) : GruzDraw.mix(0xFF7A1A, 0xFFE27A, (f - 0.5f) * 2f);
            }
            case "frost" -> GruzDraw.mix(0x5CC8FF, 0xFFFFFF, band(i, n, t * 0.8f) * 0.8f + wave(t * 3f + i) * 0.2f);
            // pustka: ciemny fiolet z pulsującą poświatą i jasnym pasem
            case "void" -> GruzDraw.mix(GruzDraw.mix(0x2A0E4A, 0x9A4CF0, wave(t * 1.8f - i * 0.5f)), 0xF0DDFF, band(i, n, t) * 0.7f);
            default -> 0xFFFFFF;
        };
    }

    private static float wave(float x) { return (float) Math.sin(x) * 0.5f + 0.5f; }

    /** Jasny pasek przesuwający się po napisie */
    private static float band(int i, int n, float t) {
        float pos = (t * 0.6f % 1.6f) - 0.3f; // -0.3..1.3
        float d = Math.abs(i / (float) Math.max(1, n - 1) - pos);
        return Math.max(0f, 1f - d * 4f);
    }

    private static int hsv(float h, float s, float v) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, v);
        return rgb & 0xFFFFFF;
    }
}
