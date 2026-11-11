package pl.gruzclient.legacy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Peleryny Gruz na 1.8.9 (od 0.2.0): gotowe obrazki HD z zasobów moda, animowane rysowane na żywo (ten sam wzór co na nowych
 * wersjach) i własne peleryny z panelu (obrazek z serwera Gruz). Bez Legacy Fabric API zasoby moda nie są paczką zasobów gry,
 * więc obrazki czytamy sami i rejestrujemy jako tekstury.
 */
public final class Capes {
    private Capes() {}

    private static final Set<String> HD = new HashSet<String>(Arrays.asList("gruz", "nebula", "sakura", "inferno", "ocean", "pumpkin", "winter"));
    private static final Set<String> ANIM = new HashSet<String>(Arrays.asList("flow", "crystals", "galaxy", "aurora", "ember"));
    private static final int SCALE = 4, W = 64 * SCALE, H = 32 * SCALE;
    private static final Map<String, Identifier> IDS = new HashMap<String, Identifier>();
    private static final Map<String, NativeImageBackedTexture> LIVE = new HashMap<String, NativeImageBackedTexture>();
    private static final Set<String> loading = new HashSet<String>(), failed = new HashSet<String>();
    private static int frame;

    private static String value(String set, String key) {
        if (set == null) return null;
        for (String part : set.split(";")) {
            int i = part.indexOf('=');
            if (i > 0 && part.substring(0, i).equals(key)) return part.substring(i + 1);
        }
        return null;
    }

    /** Peleryna Gruz gracza albo null (wtedy gra rysuje zwykłą pelerynę z Mojanga, jeśli gracz ją ma) */
    public static Identifier forPlayer(AbstractClientPlayerEntity p) {
        if (!Mods.cfg.capes) return null;
        MinecraftClient mc = MinecraftClient.getInstance();
        String set = p == mc.player ? GruzNet.myCosmetics : p.getUuid() == null ? null : GruzNet.COSMETICS.get(p.getUuid());
        String cape = value(set, "cape");
        if (cape == null || cape.isEmpty() || cape.equals("none")) return null;
        int color = 0xB45CFF;
        try { String c = value(set, "color"); if (c != null) color = Integer.parseInt(c, 16); } catch (Exception ignored) { }
        return id(cape, color);
    }

    private static Identifier register(String key, BufferedImage img) {
        Identifier id = new Identifier("gruzclient", "cape/" + key);
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        MinecraftClient.getInstance().getTextureManager().loadTexture(id, tex);
        IDS.put(key, id);
        return id;
    }

    /** Wołane z wątku rysowania (pierwsze użycie wczytuje obrazek) */
    private static Identifier id(final String style, int color) {
        final String key = ANIM.contains(style) ? style + "_" + Integer.toHexString(color & 0xFFFFFF) : style;
        Identifier id = IDS.get(key);
        if (id != null) return id;
        if (failed.contains(key)) return null;
        if (HD.contains(style)) {
            try (InputStream in = Capes.class.getResourceAsStream("/assets/gruzclient/textures/cape/" + style + ".png")) {
                if (in != null) return register(key, ImageIO.read(in));
            } catch (Exception e) { GruzLegacy.LOG.warn("Gruz: peleryna {}: {}", style, e.toString()); }
            failed.add(key); // nie próbujemy co klatkę
            return null;
        }
        if (ANIM.contains(style)) {
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            id = register(key, img);
            NativeImageBackedTexture tex = (NativeImageBackedTexture) MinecraftClient.getInstance().getTextureManager().getTexture(id);
            LIVE.put(key, tex);
            paint(style, color, tex, 0f);
            return id;
        }
        if (style.startsWith("c_") && style.matches("[a-z0-9_]{3,40}")) {
            // własna peleryna z panelu: pobieramy w tle, do tego czasu peleryna Gruz
            if (loading.add(key)) {
                Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            String base = System.getProperty("gruz.server", "wss://gruz-server.onrender.com").replaceFirst("^ws", "http");
                            HttpURLConnection c = (HttpURLConnection) new URL(base + "/capes/" + style + ".png").openConnection();
                            c.setConnectTimeout(15000);
                            c.setReadTimeout(20000);
                            final BufferedImage img;
                            try (InputStream in = c.getInputStream()) { img = ImageIO.read(in); }
                            if (img == null || img.getWidth() > 2048 || img.getHeight() > 1024) return;
                            MinecraftClient.getInstance().submit(new Runnable() { @Override public void run() { register(key, img); } });
                        } catch (Exception e) { GruzLegacy.LOG.info("Gruz: nie pobrano peleryny {}: {}", style, e.toString()); }
                    }
                }, "Gruz cape");
                t.setDaemon(true);
                t.start();
            }
            return id("gruz", color);
        }
        return null;
    }

    /** Co tick: animowane peleryny odświeżamy 10 razy na sekundę */
    public static void tick() {
        if (++frame % 2 != 0 || LIVE.isEmpty() || MinecraftClient.getInstance().world == null) return;
        float t = frame / 20f;
        for (Map.Entry<String, NativeImageBackedTexture> e : LIVE.entrySet()) {
            String[] p = e.getKey().split("_");
            int color;
            try { color = Integer.parseInt(p[1], 16); } catch (Exception ex) { color = 0xB45CFF; }
            paint(p[0], color, e.getValue(), t);
        }
    }

    private static int mix(int a, int b, float k) {
        k = Math.max(0f, Math.min(1f, k));
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * k);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * k);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * k);
        return (r << 16) | (g << 8) | bl;
    }

    private static float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    /** Ten sam wzór co CapeTextures w modzie na nowe wersje */
    private static void paint(String style, int color, NativeImageBackedTexture tex, float t) {
        int[] px = tex.getPixels();
        int deep = mix(color, 0x000000, 0.72f), dark = mix(color, 0x000000, 0.45f), hot = mix(color, 0xFFFFFF, 0.5f);
        for (int py = 0; py < 17 * SCALE; py++) {
            for (int pxx = 0; pxx < 22 * SCALE; pxx++) {
                float fx = (pxx + 0.5f) / SCALE, fy = (py + 0.5f) / SCALE;
                int x = (int) fx, y = (int) fy;
                float u = fx < 11 ? fx : fx - 11, v = fy;
                int rgb;
                if (style.equals("flow")) {
                    float w = (float) Math.sin(u * 0.55f + v * 0.35f - t * 2.4f) * 0.5f + 0.5f;
                    float w2 = (float) Math.sin(u * 0.9f - v * 0.22f + t * 1.7f) * 0.5f + 0.5f;
                    float k = Math.min(1f, w * 0.65f + w2 * 0.35f) * (0.35f + v / 16f * 0.75f);
                    rgb = mix(deep, k > 0.75f ? hot : color, Math.min(1f, k * 1.25f));
                } else if (style.equals("crystals")) {
                    float facet = ((int) ((u + v) / 3f) % 2 == 0) ? 0.25f : 0.1f;
                    float facet2 = ((int) ((u - v + 40) / 4f) % 2 == 0) ? 0.15f : 0f;
                    rgb = mix(deep, color, facet + facet2 + v / 16f * 0.25f);
                    float h = hash(x, y);
                    if (h > 0.9f) { float tw = (float) Math.sin(t * 4f + h * 40f) * 0.5f + 0.5f; rgb = mix(rgb, 0xFFFFFF, tw * tw); }
                } else if (style.equals("galaxy")) {
                    float neb = (float) (Math.sin(u * 0.4f + t * 0.3f) + Math.sin(v * 0.5f - t * 0.2f) + Math.sin((u + v) * 0.3f)) / 6f + 0.5f;
                    rgb = mix(0x05030A, dark, neb * neb);
                    float h = hash(x * 7 + 3, y * 13 + 1);
                    if (h > 0.86f) { float tw = (float) Math.sin(t * 3f + h * 60f) * 0.5f + 0.5f; rgb = mix(rgb, h > 0.97f ? hot : 0xFFFFFF, 0.4f + 0.6f * tw); }
                } else if (style.equals("aurora")) {
                    float wave = (float) Math.sin(u * 0.7f + t * 1.3f + (float) Math.sin(v * 0.25f + t * 0.6f) * 1.6f);
                    float band = (float) Math.sin(v * 0.42f - t * 0.9f + u * 0.15f) * 0.5f + 0.5f;
                    int a = mix(0x2BFFA3, 0x38D6FF, band);
                    a = mix(a, 0xB45CFF, Math.max(0f, (float) Math.sin(u * 0.45f - t * 0.7f)) * 0.7f);
                    float k = Math.max(0f, wave) * (1f - v / 22f);
                    rgb = mix(0x03060C, a, Math.min(1f, k * 1.15f + 0.08f));
                    if (hash(x * 5 + 11, y * 3 + 7) > 0.95f) rgb = mix(rgb, 0xFFFFFF, 0.55f);
                } else { // ember
                    float heat = (float) (Math.sin(u * 1.1f + t * 2.0f) * 0.25f + Math.sin(v * 0.8f - t * 3.1f + u * 0.4f) * 0.25f + 0.5f);
                    heat *= v / 16f * 0.9f + 0.1f;
                    rgb = mix(0x0A0302, heat > 0.55f ? 0xFF7A1A : 0x8A1A05, Math.min(1f, heat * 1.4f));
                    float crack = Math.abs((float) Math.sin(u * 1.7f + v * 0.9f) + (float) Math.sin(u * 0.6f - v * 1.3f));
                    if (crack < 0.18f) rgb = mix(0xFFB347, 0xFFF1C2, (float) Math.sin(t * 4f + u) * 0.5f + 0.5f);
                    if (hash(x * 13, (int) (y + t * 6f) * 7) > 0.93f) rgb = mix(rgb, 0xFFE08A, 0.85f);
                }
                px[py * W + pxx] = 0xFF000000 | rgb;
            }
        }
        tex.upload();
    }
}
