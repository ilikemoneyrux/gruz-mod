package pl.gruzclient.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import pl.gruzclient.GruzClient;
import pl.gruzclient.cosmetics.CosmeticSet;
import pl.gruzclient.cosmetics.Cosmetics;
import pl.gruzclient.cosmetics.Emote;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.render.GruzDraw;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;

/**
 * Połączenie gry z serwerem Gruz (adres z launchera: -Dgruz.server=wss://…).
 * Logowanie tym samym mechanizmem co wejście na serwer Minecraft (sessionserver join),
 * potem: status, kosmetyki i emotki graczy na tym samym serwerze, wiadomości od znajomych, party.
 * Bez adresu serwera wszystko działa lokalnie (Twoje kosmetyki widzisz tylko Ty).
 */
public final class GruzNetwork {
    private GruzNetwork() {}

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static WebSocket socket;
    private static boolean connecting, authed;
    private static long nextAttempt = 0;
    private static int failures = 0;
    private static String lastPresence = "";
    private static final Set<UUID> looked = new HashSet<>();
    private static int ticks = 0;
    private static final Map<UUID, String> friendNames = new HashMap<>();
    private static UUID lastDmFrom;

    public static String serverUrl() { return System.getProperty("gruz.server", "").trim(); }
    public static boolean enabled() { return !serverUrl().isEmpty(); }
    public static boolean connected() { return socket != null && authed; }

    // ---------------- pętla ----------------

    /** Co tick klienta: łączenie z backoffem, status, pytania o kosmetyki widzianych graczy */
    public static void tick() {
        if (!enabled()) return;
        long now = System.currentTimeMillis();
        if (socket == null && !connecting && now >= nextAttempt) connect();
        if (!connected()) return;
        if (++ticks % 20 == 0) sendPresence(false);
        if (ticks % 100 == 0) lookupVisiblePlayers();
    }

    private static void connect() {
        connecting = true;
        HTTP.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(serverUrl()), new Listener())
                .whenComplete((ws, err) -> {
                    connecting = false;
                    if (err != null) { scheduleRetry(); return; }
                    socket = ws;
                    JsonObject hello = new JsonObject();
                    hello.addProperty("t", "hello");
                    hello.addProperty("client", "game");
                    send(hello);
                });
    }

    private static void scheduleRetry() {
        socket = null;
        authed = false;
        failures = Math.min(failures + 1, 6);
        nextAttempt = System.currentTimeMillis() + 5000L * (1L << failures); // 10 s … 5 min
    }

    private static void send(JsonObject o) {
        WebSocket ws = socket;
        if (ws != null) ws.sendText(GSON.toJson(o), true);
    }

    private static JsonObject msg(String type) {
        JsonObject o = new JsonObject();
        o.addProperty("t", type);
        return o;
    }

    // ---------------- wysyłanie ----------------

    public static void sendPresence(boolean force) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String state = mc.world == null ? "menu" : mc.isInSingleplayer() ? "singleplayer" : "multiplayer";
        String server = "multiplayer".equals(state) && mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : null;
        String key = state + "|" + server;
        if (!force && key.equals(lastPresence)) return;
        lastPresence = key;
        JsonObject o = msg("presence");
        o.addProperty("state", state);
        if (server != null) o.addProperty("server", server);
        send(o);
        if (!"multiplayer".equals(state)) Cosmetics.REMOTE.clear();
        looked.clear();
    }

    public static void publishCosmetics(CosmeticSet set) {
        if (!connected()) return;
        JsonObject o = msg("cosmetics");
        o.addProperty("set", set.encode());
        send(o);
    }

    public static void publishEmote(Emote e) {
        if (!connected()) return;
        JsonObject o = msg("emote");
        o.addProperty("id", e.id);
        send(o);
    }

    public static void sendDm(String nameOrUuid, String text) {
        if (!connected()) { chat(Text.literal(Lang.t("net.offline")).formatted(Formatting.RED)); return; }
        UUID target = null;
        for (Map.Entry<UUID, String> f : friendNames.entrySet()) if (f.getValue().equalsIgnoreCase(nameOrUuid)) target = f.getKey();
        if (target == null) { chat(Text.literal(Lang.t("net.notFriend").replace("{name}", nameOrUuid)).formatted(Formatting.RED)); return; }
        JsonObject o = msg("dm");
        o.addProperty("to", target.toString());
        o.addProperty("text", text);
        send(o);
    }

    public static void reply(String text) {
        if (lastDmFrom == null) { chat(Text.literal(Lang.t("net.noReply")).formatted(Formatting.GRAY)); return; }
        sendDm(friendNames.getOrDefault(lastDmFrom, lastDmFrom.toString()), text);
    }

    public static void partyWarp() {
        if (!connected()) { chat(Text.literal(Lang.t("net.offline")).formatted(Formatting.RED)); return; }
        send(msg("party.warp"));
    }

    /** Pyta serwer o kosmetyki graczy w zasięgu, których jeszcze nie znamy */
    private static void lookupVisiblePlayers() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        JsonArray ids = new JsonArray();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || looked.contains(p.getUuid())) continue;
            looked.add(p.getUuid());
            ids.add(p.getUuid().toString());
        }
        if (ids.isEmpty()) return;
        JsonObject o = msg("lookup");
        o.add("uuids", ids);
        send(o);
    }

    // ---------------- odbieranie ----------------

    private static void chat(Text t) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(prefix().append(t), false);
    }

    private static MutableText prefix() {
        return Text.literal("◆ Gruz ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(GruzDraw.NEON)));
    }

    private static void handle(JsonObject m) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String t = m.get("t").getAsString();
        switch (t) {
            case "challenge" -> {
                String serverId = m.get("serverId").getAsString();
                // potwierdzenie konta przez Mojang (jak przy wejściu na serwer) – w tle, żeby nie zacinać gry
                CompletableFuture.runAsync(() -> {
                    try {
                        var session = mc.getSession();
                        mc.getSessionService().joinServer(session.getUuidOrNull(), session.getAccessToken(), serverId);
                        JsonObject o = msg("auth");
                        o.addProperty("name", session.getUsername());
                        send(o);
                    } catch (Exception e) {
                        GruzClient.LOG.warn("Gruz: nie udało się potwierdzić konta: {}", e.toString());
                        WebSocket ws = socket;
                        if (ws != null) ws.abort();
                        scheduleRetry();
                    }
                });
            }
            case "welcome" -> {
                authed = true;
                failures = 0;
                lastPresence = "";
                sendPresence(true);
                publishCosmetics(pl.gruzclient.module.Modules.COSMETICS.current());
            }
            case "cosmetics" -> {
                JsonObject map = m.getAsJsonObject("map");
                for (String key : map.keySet()) {
                    try {
                        String v = map.get(key).getAsString();
                        UUID id = UUID.fromString(key);
                        Cosmetics.REMOTE.put(id, CosmeticSet.decode(v));
                    } catch (Exception ignored) { }
                }
            }
            case "emote" -> {
                Emote e = Emote.byId(m.get("id").getAsString());
                if (e != null) Cosmetics.EMOTES.put(UUID.fromString(m.get("uuid").getAsString()), new Emote.Playing(e, System.currentTimeMillis()));
            }
            case "friends" -> {
                friendNames.clear();
                for (var el : m.getAsJsonArray("friends")) {
                    JsonObject f = el.getAsJsonObject();
                    friendNames.put(UUID.fromString(f.get("uuid").getAsString()), f.get("name").getAsString());
                }
            }
            case "friend.status" -> {
                JsonObject f = m.getAsJsonObject("friend");
                String name = f.get("name").getAsString();
                boolean wasKnown = friendNames.containsValue(name);
                friendNames.put(UUID.fromString(f.get("uuid").getAsString()), name);
                if (wasKnown && f.has("online") && f.get("online").getAsBoolean() && "launcher".equals(str(f, "state"))) {
                    chat(Text.literal(Lang.t("net.friendOnline").replace("{name}", name)).formatted(Formatting.GRAY));
                }
            }
            case "dm" -> {
                String from = m.get("from").getAsString();
                boolean mine = mc.getSession().getUuidOrNull() != null && from.equals(mc.getSession().getUuidOrNull().toString());
                String other = mine ? str(m, "toName") : str(m, "fromName");
                if (!mine) lastDmFrom = UUID.fromString(from);
                MutableText line = Text.literal(mine ? "→ " + other + ": " : other + " → " + Lang.t("net.you") + ": ").formatted(Formatting.LIGHT_PURPLE)
                        .append(Text.literal(m.has("text") ? m.get("text").getAsString()
                                : m.has("voice") ? Lang.t("net.voice") : "[" + str(m, "sticker").toUpperCase() + "]").formatted(Formatting.WHITE));
                chat(line);
            }
            case "notify" -> {
                String kind = str(m, "kind"), name = str(m, "name");
                if ("badge".equals(kind) && m.has("names")) name = m.getAsJsonObject("names").get(Lang.current()).getAsString();
                chat(Text.literal(Lang.t("net." + kind).replace("{name}", name)).formatted(Formatting.GRAY));
            }
            case "party.invite" -> chat(Text.literal(Lang.t("net.partyInvite").replace("{name}", str(m, "from"))).formatted(Formatting.GRAY));
            case "party.warp" -> {
                String address = str(m, "address");
                chat(Text.literal(Lang.t("net.warping").replace("{name}", str(m, "from")).replace("{server}", address)).formatted(Formatting.GRAY));
                warpTo(address);
            }
            case "error" -> {
                String code = str(m, "code");
                if ("AUTH_FAILED".equals(code)) { GruzClient.LOG.warn("Gruz: serwer odrzucił logowanie"); scheduleRetry(); }
                else chat(Text.literal(Lang.t("net.err." + code).replace("{name}", str(m, "name"))).formatted(Formatting.RED));
            }
            default -> { }
        }
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    /** Party: przejście na serwer lidera */
    private static void warpTo(String address) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ServerInfo current = mc.getCurrentServerEntry();
        if (current != null && current.address.equalsIgnoreCase(address)) return;
        if (!ServerAddress.isValid(address)) return;
        if (mc.world != null) {
            mc.world.disconnect();
            mc.disconnect();
        }
        ServerInfo info = new ServerInfo(address, address, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(new TitleScreen(), mc, ServerAddress.parse(address), info, false, null);
    }

    // ---------------- WebSocket ----------------

    private static final class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String text = buffer.toString();
                buffer.setLength(0);
                try {
                    JsonObject m = JsonParser.parseString(text).getAsJsonObject();
                    MinecraftClient.getInstance().execute(() -> {
                        try { handle(m); } catch (Exception e) { GruzClient.LOG.debug("Gruz: błąd wiadomości", e); }
                    });
                } catch (Exception ignored) { }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            MinecraftClient.getInstance().execute(GruzNetwork::scheduleRetry);
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            MinecraftClient.getInstance().execute(GruzNetwork::scheduleRetry);
        }
    }
}
