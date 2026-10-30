package pl.gruzclient.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pl.gruzclient.i18n.Lang;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Najnowszy news z serwera Gruz (/news.json) do karty w menu głównym. */
public final class NewsFeed {
    private NewsFeed() {}

    public record Item(String tag, String title, String body) {}

    private static volatile Item latest;
    private static boolean requested;

    public static Item latest() {
        if (!requested) fetch();
        return latest;
    }

    private static String pick(JsonElement e) {
        if (e == null || e.isJsonNull()) return "";
        if (e.isJsonPrimitive()) return e.getAsString();
        JsonObject o = e.getAsJsonObject();
        String l = Lang.current();
        if (o.has(l)) return o.get(l).getAsString();
        return o.has("pl") ? o.get("pl").getAsString() : "";
    }

    private static void fetch() {
        requested = true;
        String srv = GruzNetwork.serverUrl();
        if (!srv.matches("(?i)^wss?://.+")) return;
        try {
            URI ws = URI.create(srv);
            String scheme = "wss".equalsIgnoreCase(ws.getScheme()) ? "https" : "http";
            URI uri = new URI(scheme, null, ws.getHost(), ws.getPort(), "/news.json", null, null);
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
                    .sendAsync(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> {
                        if (r.statusCode() != 200) return;
                        JsonArray arr = JsonParser.parseString(r.body()).getAsJsonArray();
                        if (arr.isEmpty()) return;
                        JsonObject n = arr.get(0).getAsJsonObject();
                        latest = new Item(pick(n.get("tag")), pick(n.get("title")), pick(n.get("body")));
                    })
                    .exceptionally(e -> null);
        } catch (Exception ignored) {
            // brak newsów – karta pokaże wskazówki
        }
    }
}
