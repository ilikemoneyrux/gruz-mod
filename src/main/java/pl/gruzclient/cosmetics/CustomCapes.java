package pl.gruzclient.cosmetics;

import pl.gruzclient.GruzClient;
import pl.gruzclient.compat.Compat;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.network.GruzNetwork;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Własne peleryny dodawane w panelu admina serwera Gruz (bez nowej wersji moda).
 * Launcher podaje listę: -Dgruz.capes=c_smok~Smok~Dragon,… (nazwy zakodowane jak w URL).
 * Obrazek pobieramy z serwera Gruz (/capes/<id>.png) przy pierwszym użyciu – też dla peleryn
 * innych graczy dodanych już po starcie gry. Do czasu pobrania widać zwykłą pelerynę Gruz.
 */
public final class CustomCapes {
    private CustomCapes() {}

    private static final Pattern ID = Pattern.compile("^c_[a-z0-9_]{1,32}$");
    /** Peleryny z launchera, w kolejności z panelu */
    public static final List<String> IDS = new ArrayList<>();
    private static final Map<String, ResourceLocation> LOADED = new ConcurrentHashMap<>();
    /** id -> czas, po którym można spróbować pobrać ponownie (0 = trwa pobieranie) */
    private static final Map<String, Long> PENDING = new ConcurrentHashMap<>();
    private static HttpClient http;

    static {
        String prop = System.getProperty("gruz.capes", "");
        for (String entry : prop.split(",")) {
            String[] p = entry.split("~", -1);
            if (p.length < 1 || !isId(p[0]) || IDS.contains(p[0])) continue;
            IDS.add(p[0]);
            String pl = p.length > 1 ? decode(p[1]) : p[0];
            String en = p.length > 2 ? decode(p[2]) : pl;
            Lang.define("cape." + p[0], pl, en.isBlank() ? pl : en);
        }
    }

    private static String decode(String s) {
        try { return URLDecoder.decode(s, StandardCharsets.UTF_8); } catch (Exception e) { return s; }
    }

    /** Czy to identyfikator własnej peleryny (c_…) */
    public static boolean isId(String id) { return id != null && ID.matcher(id).matches(); }

    /** Zwykła peleryna Gruz (gdy obrazek jeszcze się pobiera albo nie ma internetu) */
    private static ResourceLocation fallback() { return Compat.id(GruzClient.MOD_ID, "textures/cape/gruz.png"); }

    /** Tekstura peleryny – pobiera ją w tle przy pierwszym użyciu */
    public static ResourceLocation id(String capeId) {
        ResourceLocation loaded = LOADED.get(capeId);
        if (loaded != null) return loaded;
        Long retry = PENDING.get(capeId);
        if (retry == null || (retry > 0 && System.currentTimeMillis() > retry)) download(capeId);
        return fallback();
    }

    /** wss://serwer -> https://serwer/capes/<id>.png */
    private static URI url(String capeId) throws Exception {
        String srv = GruzNetwork.serverUrl();
        if (!srv.matches("(?i)^wss?://.+")) return null;
        URI ws = URI.create(srv);
        String scheme = "wss".equalsIgnoreCase(ws.getScheme()) ? "https" : "http";
        return new URI(scheme, null, ws.getHost(), ws.getPort(), "/capes/" + capeId + ".png", null, null);
    }

    private static void download(String capeId) {
        PENDING.put(capeId, 0L);
        try {
            URI uri = url(capeId);
            if (uri == null) { PENDING.put(capeId, Long.MAX_VALUE); return; }
            if (http == null) http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();
            http.sendAsync(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
                    .thenAccept(r -> {
                        if (r.statusCode() != 200 || r.body().length == 0 || r.body().length > 4 * 1024 * 1024) { failed(capeId, r.statusCode() == 404 ? 600_000 : 60_000); return; }
                        byte[] bytes = r.body();
                        Minecraft.getInstance().execute(() -> register(capeId, bytes));
                    })
                    .exceptionally(e -> { failed(capeId, 60_000); return null; });
        } catch (Exception e) {
            failed(capeId, 60_000);
        }
    }

    private static void failed(String capeId, long waitMs) { PENDING.put(capeId, System.currentTimeMillis() + waitMs); }

    /** Na wątku gry: obrazek -> tekstura */
    private static void register(String capeId, byte[] bytes) {
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
            if (img.getWidth() != img.getHeight() * 2 || img.getWidth() < 64) {
                img.close();
                failed(capeId, Long.MAX_VALUE / 2);
                return;
            }
            ResourceLocation id = Compat.id(GruzClient.MOD_ID, "cape/custom/" + capeId);
            DynamicTexture t = Compat.texture("gruz_cape_" + capeId, img, false);
            Minecraft.getInstance().getTextureManager().register(id, t);
            LOADED.put(capeId, id);
            PENDING.remove(capeId);
        } catch (Exception e) {
            GruzClient.LOG.warn("Nie udało się wczytać peleryny {}: {}", capeId, e.toString());
            failed(capeId, 600_000);
        }
    }
}
