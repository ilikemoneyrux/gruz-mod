package pl.gruzclient.hypixel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pl.gruzclient.GruzClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Statystyki BedWars z oficjalnego API Hypixela (gwiazdki i FKDR) pokazywane w tabie.
 * Klucz API podajesz w launcherze (Ustawienia → Hypixel), trafia do gry jako -Dgruz.hypixel.
 * Zapytania idą w kolejce (max ~1 na 1,2 s), wyniki są trzymane w pamięci przez 10 minut.
 */
public final class HypixelStats {
    private HypixelStats() {}

    public record Stats(int stars, double fkdr, double wlr, boolean nick, long at) {}

    private static final Map<UUID, Stats> CACHE = new ConcurrentHashMap<>();
    private static final ArrayDeque<UUID> QUEUE = new ArrayDeque<>();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static long nextAt = 0, backoffUntil = 0;
    private static boolean inFlight = false;
    private static boolean badKey = false;

    public static String key() { return System.getProperty("gruz.hypixel", "").trim(); }

    public static boolean onHypixel() {
        ServerData s = Minecraft.getInstance().getCurrentServer();
        return s != null && s.ip != null && s.ip.toLowerCase().contains("hypixel.net");
    }

    public static boolean available() { return !key().isEmpty() && !badKey && onHypixel(); }

    /** Wynik z pamięci albo null (wtedy dopisujemy gracza do kolejki) */
    public static Stats get(UUID id) {
        if (!available() || id.version() != 4) return null; // v2 = NPC / boty Hypixela
        Stats s = CACHE.get(id);
        if (s != null && System.currentTimeMillis() - s.at() < 600_000) return s;
        synchronized (QUEUE) {
            if (!QUEUE.contains(id) && QUEUE.size() < 200) QUEUE.add(id);
        }
        return s;
    }

    /** Wołane co tick klienta */
    public static void tick() {
        if (!available() || inFlight) return;
        long now = System.currentTimeMillis();
        if (now < nextAt || now < backoffUntil) return;
        UUID id;
        synchronized (QUEUE) { id = QUEUE.poll(); }
        if (id == null) return;
        inFlight = true;
        nextAt = now + 1200;
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.hypixel.net/v2/player?uuid=" + id.toString().replace("-", "")))
                .header("API-Key", key()).timeout(Duration.ofSeconds(8)).GET().build();
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((res, err) -> {
            inFlight = false;
            if (err != null) return;
            int code = res.statusCode();
            if (code == 403) { badKey = true; GruzClient.LOG.warn("Hypixel: zły klucz API"); return; }
            if (code == 429) { backoffUntil = System.currentTimeMillis() + 60_000; synchronized (QUEUE) { QUEUE.addFirst(id); } return; }
            if (code != 200) return;
            try {
                CACHE.put(id, parse(JsonParser.parseString(res.body()).getAsJsonObject()));
            } catch (Exception e) {
                GruzClient.LOG.debug("Hypixel: błąd odpowiedzi", e);
            }
        });
    }

    private static Stats parse(JsonObject root) {
        long now = System.currentTimeMillis();
        JsonElement p = root.get("player");
        if (p == null || p.isJsonNull()) return new Stats(0, 0, 0, true, now); // brak profilu = prawie na pewno nick
        JsonObject player = p.getAsJsonObject();
        int stars = 0;
        if (player.has("achievements")) {
            JsonObject a = player.getAsJsonObject("achievements");
            if (a.has("bedwars_level")) stars = a.get("bedwars_level").getAsInt();
        }
        double fk = 0, fd = 0, w = 0, l = 0;
        if (player.has("stats") && player.getAsJsonObject("stats").has("Bedwars")) {
            JsonObject bw = player.getAsJsonObject("stats").getAsJsonObject("Bedwars");
            fk = num(bw, "final_kills_bedwars");
            fd = num(bw, "final_deaths_bedwars");
            w = num(bw, "wins_bedwars");
            l = num(bw, "losses_bedwars");
        }
        return new Stats(stars, fd == 0 ? fk : fk / fd, l == 0 ? w : w / l, false, now);
    }

    private static double num(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : 0;
    }

    /** Kolor gwiazdek jak prestiże BedWars (uproszczony) */
    public static int starColor(int stars) {
        int[] c = {0xAAAAAA, 0xFFFFFF, 0xFFAA00, 0x55FFFF, 0x00AA00, 0x00AAAA, 0xAA0000, 0xFF55FF, 0x5555FF, 0xAA00AA};
        return c[Math.min(c.length - 1, stars / 100)];
    }

    public static int fkdrColor(double f) {
        if (f >= 10) return 0xAA00AA;
        if (f >= 5) return 0xFF5555;
        if (f >= 3) return 0xFFAA00;
        if (f >= 1.5) return 0xFFFF55;
        if (f >= 0.8) return 0x55FF55;
        return 0xAAAAAA;
    }
}
