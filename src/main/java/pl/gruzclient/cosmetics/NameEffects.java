package pl.gruzclient.cosmetics;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import pl.gruzclient.render.GruzDraw;

/** Animowane nicki (kosmetyka "name"): tęcza, neon, złoto, ogień, lód. Widzą je inni gracze Gruz. */
public final class NameEffects {
    private NameEffects() {}

    /**
     * Koloruje tylko sam nick (np. w "[VIP] Gracz" ranga zostaje w kolorach serwera).
     * @param nick nazwa konta; gdy nie ma jej w tekście, kolorujemy cały tekst
     */
    public static Text apply(Text original, CosmeticSet set, String nick) {
        if (set == null || !set.hasName()) return original;
        String full = original.getString();
        if (full.isEmpty()) return original;
        int start = nick == null ? -1 : full.indexOf(nick);
        int from = start >= 0 ? start : 0, to = start >= 0 ? start + nick.length() : full.length();
        float t = (System.currentTimeMillis() % 100_000L) / 1000f;
        MutableText out = Text.empty();
        int[] pos = {0};
        original.visit((style, segment) -> {
            for (int i = 0; i < segment.length(); ) {
                int cp = segment.codePointAt(i);
                int g = pos[0];
                String ch = new String(Character.toChars(cp));
                if (g >= from && g < to) {
                    out.append(Text.literal(ch).setStyle(style.withColor(TextColor.fromRgb(color(set.name(), set.color(), g - from, to - from, t)))));
                } else {
                    out.append(Text.literal(ch).setStyle(style));
                }
                int n = Character.charCount(cp);
                i += n;
                pos[0] += n;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
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
