package pl.gruzclient.cosmetics;

import pl.gruzclient.compat.Compat;
import pl.gruzclient.GruzClient;
import pl.gruzclient.render.GruzDraw;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Animowane peleryny generowane na żywo (64x32, układ jak zwykła peleryna Minecrafta).
 * Każdy styl + kolor ma własną teksturę, odświeżaną kilka razy na sekundę.
 */
public final class CapeTextures {
    private CapeTextures() {}

    private static final Map<String, DynamicTexture> TEXTURES = new HashMap<>();
    private static final Map<String, ResourceLocation> IDS = new HashMap<>();
    private static int frame = 0;

    /** Rozdzielczość animowanych peleryn: 64x32 razy SCALE */
    private static final int SCALE = 4;

    public static ResourceLocation id(String style, int color) {
        // peleryny HD to gotowe obrazki 1024x512 z zasobów moda
        if (CosmeticSet.HD_CAPES.contains(style)) return Compat.id(GruzClient.MOD_ID, "textures/cape/" + style + ".png");
        String key = style + "_" + Integer.toHexString(color & 0xFFFFFF);
        ResourceLocation id = IDS.get(key);
        if (id != null) return id;
        DynamicTexture tex = Compat.texture("gruz_cape", 64 * SCALE, 32 * SCALE);
        id = Compat.id(GruzClient.MOD_ID, "cape/" + key);
        Minecraft.getInstance().getTextureManager().register(id, tex);
        TEXTURES.put(key, tex);
        IDS.put(key, id);
        paint(style, color, tex, 0f);
        return id;
    }

    /** Co tick: odśwież animowane peleryny (co 2 ticki = 10 klatek na sekundę) */
    public static void tick() {
        if (++frame % 2 != 0 || TEXTURES.isEmpty()) return;
        float t = frame / 20f;
        for (Map.Entry<String, DynamicTexture> e : TEXTURES.entrySet()) {
            String[] parts = e.getKey().split("_");
            int color;
            try { color = Integer.parseInt(parts[1], 16); } catch (Exception ex) { color = GruzDraw.NEON; }
            paint(parts[0], color, e.getValue(), t);
        }
    }

    private static int abgr(int rgb, int a) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    private static void paint(String style, int color, DynamicTexture tex, float t) {
        NativeImage img = tex.getPixels();
        if (img == null) return;
        int deep = GruzDraw.mix(color, 0x000000, 0.72f);
        int dark = GruzDraw.mix(color, 0x000000, 0.45f);
        int hot = GruzDraw.mix(color, 0xFFFFFF, 0.5f);
        // obszar peleryny: 22 x 17 pikseli (przód, tył i boki) w skali SCALE – gładsze wzory
        for (int py = 0; py < 17 * SCALE; py++) {
            for (int px = 0; px < 22 * SCALE; px++) {
                float fx = (px + 0.5f) / SCALE, fy = (py + 0.5f) / SCALE;
                int x = (int) fx, y = (int) fy; // całe "piksele" dla migoczących punktów
                // współrzędne "na pelerynie": u w poziomie (0..10), v w pionie (0..16)
                float u = fx < 11 ? fx : fx - 11, v = fy;
                int rgb;
                switch (style) {
                    case "flow" -> {
                        float w = (float) Math.sin(u * 0.55f + v * 0.35f - t * 2.4f) * 0.5f + 0.5f;
                        float w2 = (float) Math.sin(u * 0.9f - v * 0.22f + t * 1.7f) * 0.5f + 0.5f;
                        float k = Math.min(1f, w * 0.65f + w2 * 0.35f) * (0.35f + v / 16f * 0.75f);
                        rgb = GruzDraw.mix(deep, k > 0.75f ? hot : color, Math.min(1f, k * 1.25f));
                    }
                    case "crystals" -> {
                        // fasety: ukośne pasy + migoczące punkty
                        float facet = ((int) ((u + v) / 3f) % 2 == 0) ? 0.25f : 0.1f;
                        float facet2 = ((int) ((u - v + 40) / 4f) % 2 == 0) ? 0.15f : 0f;
                        rgb = GruzDraw.mix(deep, color, facet + facet2 + v / 16f * 0.25f);
                        float h = hash(x, y);
                        if (h > 0.9f) {
                            float tw = (float) Math.sin(t * 4f + h * 40f) * 0.5f + 0.5f;
                            rgb = GruzDraw.mix(rgb, 0xFFFFFF, tw * tw);
                        }
                    }
                    case "galaxy" -> {
                        float neb = (float) (Math.sin(u * 0.4f + t * 0.3f) + Math.sin(v * 0.5f - t * 0.2f) + Math.sin((u + v) * 0.3f)) / 6f + 0.5f;
                        rgb = GruzDraw.mix(0x05030A, dark, neb * neb);
                        float h = hash(x * 7 + 3, y * 13 + 1);
                        if (h > 0.86f) {
                            float tw = (float) Math.sin(t * 3f + h * 60f) * 0.5f + 0.5f;
                            rgb = GruzDraw.mix(rgb, h > 0.97f ? hot : 0xFFFFFF, 0.4f + 0.6f * tw);
                        }
                    }
                    case "aurora" -> {
                        // zorza: pionowe zasłony zieleni, turkusu i fioletu falujące w poziomie
                        float wave = (float) Math.sin(u * 0.7f + t * 1.3f + (float) Math.sin(v * 0.25f + t * 0.6f) * 1.6f);
                        float band = (float) Math.sin(v * 0.42f - t * 0.9f + u * 0.15f) * 0.5f + 0.5f;
                        int a = GruzDraw.mix(0x2BFFA3, 0x38D6FF, band);
                        a = GruzDraw.mix(a, 0xB45CFF, Math.max(0f, (float) Math.sin(u * 0.45f - t * 0.7f)) * 0.7f);
                        float k = Math.max(0f, wave) * (1f - v / 22f);
                        rgb = GruzDraw.mix(0x03060C, a, Math.min(1f, k * 1.15f + 0.08f));
                        if (hash(x * 5 + 11, y * 3 + 7) > 0.95f) rgb = GruzDraw.mix(rgb, 0xFFFFFF, 0.55f);
                    }
                    case "ember" -> {
                        // żar: ciemny spód, wznoszące się iskry i pulsujące pęknięcia lawy
                        float heat = (float) (Math.sin(u * 1.1f + t * 2.0f) * 0.25f + Math.sin(v * 0.8f - t * 3.1f + u * 0.4f) * 0.25f + 0.5f);
                        heat *= v / 16f * 0.9f + 0.1f;
                        rgb = GruzDraw.mix(0x0A0302, heat > 0.55f ? 0xFF7A1A : 0x8A1A05, Math.min(1f, heat * 1.4f));
                        float crack = Math.abs((float) Math.sin(u * 1.7f + v * 0.9f) + (float) Math.sin(u * 0.6f - v * 1.3f));
                        if (crack < 0.18f) rgb = GruzDraw.mix(0xFFB347, 0xFFF1C2, (float) Math.sin(t * 4f + u) * 0.5f + 0.5f);
                        float spark = hash(x * 13, (int) (y + t * 6f) * 7);
                        if (spark > 0.93f) rgb = GruzDraw.mix(rgb, 0xFFE08A, 0.85f);
                    }
                    default -> { // "gruz": gradient, neonowa ramka i kryształ na środku
                        rgb = GruzDraw.mix(0x07050C, deep, v / 16f);
                        boolean edge = u < 0.6f || (u > 9.4f && u < 10.6f) || v < 0.6f || (v > 15.4f && v < 16.6f);
                        if (edge) rgb = GruzDraw.mix(color, 0xFFFFFF, (float) (Math.sin(t * 3f + v * 0.4f) * 0.15f + 0.15f));
                        float cx = Math.abs(u - 4.5f), cy = Math.abs(v - 7.5f);
                        if (cx / 2.5f + cy / 4.5f <= 1f) {
                            float glow = (float) Math.sin(t * 2.5f) * 0.2f + 0.8f;
                            rgb = GruzDraw.mix(color, hot, (1f - (cx / 2.5f + cy / 4.5f)) * glow);
                        }
                    }
                }
                Compat.setPixelABGR(img, px, py, abgr(rgb, 255));
            }
        }
        tex.upload();
    }
}
