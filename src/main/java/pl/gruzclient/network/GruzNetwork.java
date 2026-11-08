package pl.gruzclient.network;

import pl.gruzclient.compat.Compat;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
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
    /** Ostatni znany status znajomego: "" = offline, inaczej launcher/menu/singleplayer/multiplayer + "|" + serwer */
    private static final Map<UUID, String> friendState = new HashMap<>();
    private static UUID lastDmFrom;
    /** Ostatnie zaproszenie do party (do .party accept) */
    private static String inviteId;
    private static String inviteFrom = "";
    private static long inviteAt;
    /** Twoje party z serwera (null = brak) */
    private static JsonObject party;

    public static String serverUrl() { return System.getProperty("gruz.server", "").trim(); }
    public static boolean enabled() { return !serverUrl().isEmpty(); }
    public static boolean connected() { return socket != null && authed; }

    // ---------------- pętla ----------------

    /** Co tick klienta: łączenie z backoffem, status, pytania o kosmetyki widzianych graczy */
    public static void tick() {
        if (!enabled()) return;
        Shots.tick();
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

    /** Kolejka wysyłania: WebSocket w Javie pozwala na jedną wysyłkę naraz (duży screen nie może wywrócić gry) */
    private static CompletableFuture<?> sendChain = CompletableFuture.completedFuture(null);

    private static synchronized void send(JsonObject o) {
        WebSocket ws = socket;
        if (ws == null) return;
        String text = GSON.toJson(o);
        sendChain = sendChain.handle((r, e) -> null)
                .thenCompose(x -> ws.sendText(text, true))
                .exceptionally(e -> null);
    }

    /** Wysyłka z innych klas (np. Shots – z wątku w tle) */
    static void sendRaw(JsonObject o) { send(o); }

    private static JsonObject msg(String type) {
        JsonObject o = new JsonObject();
        o.addProperty("t", type);
        return o;
    }

    // ---------------- wysyłanie ----------------

    public static void sendPresence(boolean force) {
        Minecraft mc = Minecraft.getInstance();
        String state = mc.level == null ? "menu" : mc.isLocalServer() ? "singleplayer" : "multiplayer";
        String server = "multiplayer".equals(state) && mc.getCurrentServer() != null ? mc.getCurrentServer().ip : null;
        String key = state + "|" + server;
        if (!force && key.equals(lastPresence)) return;
        lastPresence = key;
        JsonObject o = msg("presence");
        o.addProperty("state", state);
        if (server != null) o.addProperty("server", server);
        send(o);
        if (!"multiplayer".equals(state)) Cosmetics.REMOTE.clear();
        looked.clear();
        if (server != null) friendsHere(server);
    }

    /** Po wejściu na serwer: którzy znajomi z Gruz już tu grają */
    private static void friendsHere(String server) {
        if (!pl.gruzclient.module.Modules.BETTER_CHAT.friendAlerts.value) return;
        java.util.List<String> here = new java.util.ArrayList<>();
        for (Map.Entry<UUID, String> e : friendState.entrySet()) {
            String s = e.getValue();
            if (s.startsWith("multiplayer|") && sameServer(s.substring(s.indexOf('|') + 1), server)) here.add(friendNames.getOrDefault(e.getKey(), "?"));
        }
        if (!here.isEmpty()) chat(Component.literal(Lang.t("net.friendsHere").replace("{names}", String.join(", ", here))).withStyle(ChatFormatting.GRAY));
    }

    public static void publishCosmetics(CosmeticSet set) {
        if (!connected()) return;
        JsonObject o = msg("cosmetics");
        o.addProperty("set", set.encode());
        send(o);
    }

    /** Emotka we dwoje: zaproszenie dla gracza obok i przyjęcie (Duo.java) */
    public static void sendDuoInvite(UUID to, String emote) {
        if (!connected()) return;
        JsonObject o = msg("duo.invite");
        o.addProperty("to", to.toString());
        o.addProperty("emote", emote);
        send(o);
    }

    public static void sendDuoAccept(UUID from) {
        if (!connected()) return;
        JsonObject o = msg("duo.accept");
        o.addProperty("from", from.toString());
        send(o);
    }

    public static void publishEmote(Emote e) {
        if (!connected()) return;
        JsonObject o = msg("emote");
        o.addProperty("id", e.id);
        send(o);
    }

    public static void sendDm(String nameOrUuid, String text) {
        if (!connected()) { chat(Component.literal(Lang.t("net.offline")).withStyle(ChatFormatting.RED)); return; }
        UUID target = null;
        for (Map.Entry<UUID, String> f : friendNames.entrySet()) if (f.getValue().equalsIgnoreCase(nameOrUuid)) target = f.getKey();
        if (target == null) { chat(Component.literal(Lang.t("net.notFriend").replace("{name}", nameOrUuid)).withStyle(ChatFormatting.RED)); return; }
        JsonObject o = msg("dm");
        o.addProperty("to", target.toString());
        o.addProperty("text", text);
        send(o);
    }

    public static void reply(String text) {
        if (lastDmFrom == null) { chat(Component.literal(Lang.t("net.noReply")).withStyle(ChatFormatting.GRAY)); return; }
        sendDm(friendNames.getOrDefault(lastDmFrom, lastDmFrom.toString()), text);
    }

    // ---------------- party z czatu (.party ...) ----------------

    private static UUID friendByName(String name) {
        for (Map.Entry<UUID, String> f : friendNames.entrySet()) if (f.getValue().equalsIgnoreCase(name)) return f.getKey();
        return null;
    }

    /** .party invite Nick – zaprasza znajomego z Gruz do Twojego party */
    public static void partyInvite(String name) {
        if (!connected()) { chat(Component.literal(Lang.t("net.offline")).withStyle(ChatFormatting.RED)); return; }
        UUID target = friendByName(name);
        if (target == null) { chat(Component.literal(Lang.t("net.notFriend").replace("{name}", name)).withStyle(ChatFormatting.RED)); return; }
        JsonObject o = msg("party.invite");
        o.addProperty("uuid", target.toString());
        send(o);
        chat(Component.literal(Lang.t("net.partyInvited").replace("{name}", friendNames.get(target))).withStyle(ChatFormatting.GRAY));
    }

    /** .party accept – przyjmuje ostatnie zaproszenie (ważne 5 minut) */
    public static void partyAccept() {
        if (!connected()) { chat(Component.literal(Lang.t("net.offline")).withStyle(ChatFormatting.RED)); return; }
        if (inviteId == null || System.currentTimeMillis() - inviteAt > 5 * 60_000L) {
            chat(Component.literal(Lang.t("net.noInvite")).withStyle(ChatFormatting.GRAY));
            return;
        }
        JsonObject o = msg("party.accept");
        o.addProperty("id", inviteId);
        send(o);
        inviteId = null;
    }

    public static void partyLeave() {
        if (!connected()) { chat(Component.literal(Lang.t("net.offline")).withStyle(ChatFormatting.RED)); return; }
        if (party == null) { chat(Component.literal(Lang.t("net.noParty")).withStyle(ChatFormatting.GRAY)); return; }
        send(msg("party.leave"));
        chat(Component.literal(Lang.t("net.partyLeft")).withStyle(ChatFormatting.GRAY));
    }

    /** .party – kto jest w Twoim party i gdzie gra */
    public static void partyInfo() {
        if (party == null) { chat(Component.literal(Lang.t("net.noParty")).withStyle(ChatFormatting.GRAY)); return; }
        String leader = str(party, "leader");
        MutableComponent line = Component.literal(Lang.t("net.partyList")).withStyle(ChatFormatting.GRAY);
        boolean first = true;
        for (var el : party.getAsJsonArray("members")) {
            JsonObject f = el.getAsJsonObject();
            boolean on = f.has("online") && f.get("online").getAsBoolean();
            line.append(Component.literal(first ? " " : ", ").withStyle(ChatFormatting.DARK_GRAY));
            line.append(Component.literal((str(f, "uuid").equals(leader) ? "★" : "") + str(f, "name"))
                    .withStyle(on ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
            String srv = str(f, "server");
            if (on && !srv.isEmpty()) line.append(Component.literal(" (" + srv + ")").withStyle(ChatFormatting.DARK_GRAY));
            first = false;
        }
        chat(line);
    }

    /** Przycisk w wiadomości na czacie: kliknięcie wpisuje komendę do pola czatu */
    private static MutableComponent button(String label, String command, ChatFormatting color) {
        return Compat.suggest(Component.literal("[" + label + "]").withStyle(color, ChatFormatting.BOLD), command);
    }

    // ---------------- powiadomienia o znajomych ----------------

    private static String stateKey(JsonObject f) {
        if (!f.has("online") || !f.get("online").getAsBoolean()) return "";
        return str(f, "state") + "|" + str(f, "server");
    }

    private static boolean inGame(String key) {
        return key.startsWith("menu|") || key.startsWith("singleplayer|") || key.startsWith("multiplayer|");
    }

    /** Adres serwera bez portu i kropek na końcu, małymi literami (mc.hypixel.net:25565 -> mc.hypixel.net) */
    private static String host(String s) {
        String h = s == null ? "" : s.trim().toLowerCase(java.util.Locale.ROOT);
        if (h.chars().filter(ch -> ch == ':').count() == 1) h = h.substring(0, h.indexOf(':'));
        while (h.endsWith(".")) h = h.substring(0, h.length() - 1);
        return h;
    }

    /** Ten sam serwer, także gdy jeden wszedł przez subdomenę (mc.hypixel.net i hypixel.net) */
    private static boolean sameServer(String a, String b) {
        a = host(a); b = host(b);
        if (a.isEmpty() || b.isEmpty()) return false;
        return a.equals(b) || a.endsWith("." + b) || b.endsWith("." + a);
    }

    /** Nowy status znajomego -> krótka wiadomość na czacie (wejście do gry, ten sam serwer co Ty) */
    private static void friendChanged(String name, String before, String now) {
        if (before == null || before.equals(now) || now.isEmpty()) return;
        if (!pl.gruzclient.module.Modules.BETTER_CHAT.friendAlerts.value) return;
        Minecraft mc = Minecraft.getInstance();
        String server = now.substring(now.indexOf('|') + 1);
        String mine = mc.getCurrentServer() != null && mc.level != null && !mc.isLocalServer() ? mc.getCurrentServer().ip : "";
        String key;
        if (now.startsWith("multiplayer|") && sameServer(server, mine)
                && !(before.startsWith("multiplayer|") && sameServer(before.substring(before.indexOf('|') + 1), mine))) key = "net.friendHere";
        else if (inGame(now) && !inGame(before)) key = "net.friendGame";
        else if (before.isEmpty()) key = "net.friendOnline";
        else return;
        MutableComponent line = Component.literal(Lang.t(key).replace("{name}", name)).withStyle(ChatFormatting.GRAY);
        if (!"net.friendHere".equals(key) && mc.level != null) {
            line.append(Component.literal("  ")).append(button(Lang.t("net.btnInvite"), ".party invite " + name, ChatFormatting.LIGHT_PURPLE));
        }
        chat(line);
    }

    public static void partyWarp() {
        if (!connected()) { chat(Component.literal(Lang.t("net.offline")).withStyle(ChatFormatting.RED)); return; }
        send(msg("party.warp"));
    }

    /** Pyta serwer o kosmetyki graczy w zasięgu, których jeszcze nie znamy */
    private static void lookupVisiblePlayers() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        JsonArray ids = new JsonArray();
        for (Player p : mc.level.players()) {
            if (p == mc.player || looked.contains(p.getUUID())) continue;
            looked.add(p.getUUID());
            ids.add(p.getUUID().toString());
        }
        // gracze z listy Tab (także daleko od Ciebie) – żeby ikonka Gruz była przy wszystkich na serwerze
        var conn = mc.getConnection();
        if (conn != null) {
            for (var info : conn.getOnlinePlayers()) {
                if (ids.size() >= 100) break;
                UUID id = Compat.profileId(info.getProfile());
                if (id == null || id.equals(mc.getUser().getProfileId()) || looked.contains(id)) continue;
                looked.add(id);
                ids.add(id.toString());
            }
        }
        if (ids.isEmpty()) return;
        JsonObject o = msg("lookup");
        o.add("uuids", ids);
        send(o);
    }

    // ---------------- odbieranie ----------------

    static void chat(Component t) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) Compat.chat(mc.player, prefix().append(t), false);
    }

    static MutableComponent prefix() {
        return Component.literal("◆ Gruz ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(GruzDraw.NEON)));
    }

    private static void handle(JsonObject m) {
        Minecraft mc = Minecraft.getInstance();
        String t = m.get("t").getAsString();
        switch (t) {
            case "challenge" -> {
                String serverId = m.get("serverId").getAsString();
                // potwierdzenie konta przez Mojang (jak przy wejściu na serwer) – w tle, żeby nie zacinać gry
                CompletableFuture.runAsync(() -> {
                    try {
                        var session = mc.getUser();
                        Compat.joinServer(mc, serverId);
                        JsonObject o = msg("auth");
                        o.addProperty("name", session.getName());
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
                party = m.has("party") && m.get("party").isJsonObject() ? m.getAsJsonObject("party") : null;
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
                if (e != null) {
                    UUID who = UUID.fromString(m.get("uuid").getAsString());
                    Emote.Playing play = new Emote.Playing(e, System.currentTimeMillis());
                    Cosmetics.EMOTES.put(who, play);
                    // zginanie przez bibliotekę animacji – na wątku gry
                    net.minecraft.client.Minecraft.getInstance().execute(() -> pl.gruzclient.cosmetics.EmoteAnim.start(who, play));
                }
            }
            case "serverRules" -> ServerRules.update(m.getAsJsonArray("rules"));
            case "shot.done" -> Shots.onDone(str(m, "url"));
            case "shot.error" -> Shots.onError(str(m, "code"));
            case "duo.invite" -> pl.gruzclient.cosmetics.Duo.onInvite(UUID.fromString(str(m, "from")), str(m, "name"), str(m, "emote"));
            case "duo.start" -> pl.gruzclient.cosmetics.Duo.onStart(UUID.fromString(str(m, "a")), UUID.fromString(str(m, "b")), str(m, "emote"));
            case "friends" -> {
                friendNames.clear();
                for (var el : m.getAsJsonArray("friends")) {
                    JsonObject f = el.getAsJsonObject();
                    UUID id = UUID.fromString(f.get("uuid").getAsString());
                    friendNames.put(id, f.get("name").getAsString());
                    // pełna lista tylko zapamiętuje statusy – powiadamiamy dopiero o zmianach
                    friendState.put(id, stateKey(f));
                }
            }
            case "friend.status" -> {
                JsonObject f = m.getAsJsonObject("friend");
                String name = f.get("name").getAsString();
                UUID id = UUID.fromString(f.get("uuid").getAsString());
                friendNames.put(id, name);
                String now = stateKey(f);
                friendChanged(name, friendState.get(id), now);
                friendState.put(id, now);
            }
            case "party" -> party = m.has("party") && m.get("party").isJsonObject() ? m.getAsJsonObject("party") : null;
            case "dm" -> {
                String from = m.get("from").getAsString();
                boolean mine = mc.getUser().getProfileId() != null && from.equals(mc.getUser().getProfileId().toString());
                String other = mine ? str(m, "toName") : str(m, "fromName");
                if (!mine) lastDmFrom = UUID.fromString(from);
                MutableComponent line = Component.literal(mine ? "→ " + other + ": " : other + " → " + Lang.t("net.you") + ": ").withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal(m.has("text") ? m.get("text").getAsString()
                                : m.has("voice") ? Lang.t("net.voice") : "[" + str(m, "sticker").toUpperCase() + "]").withStyle(ChatFormatting.WHITE));
                chat(line);
            }
            case "notify" -> {
                String kind = str(m, "kind"), name = str(m, "name");
                if (m.has("names") && m.get("names").isJsonObject() && m.getAsJsonObject("names").has(Lang.current())) {
                    name = m.getAsJsonObject("names").get(Lang.current()).getAsString();
                }
                if ("wishSale".equals(kind)) {
                    chat(Component.literal(Lang.t("net.wishSale").replace("{name}", name).replace("{off}", str(m, "off")) + " ").withStyle(ChatFormatting.GOLD)
                            .append(Compat.openUrl(Component.literal("gruzclient.com/cosmetics").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.UNDERLINE),
                                    "https://gruzclient.com/cosmetics#" + str(m, "cosmetic"))));
                    return;
                }
                chat(Component.literal(Lang.t("net." + kind).replace("{name}", name)).withStyle(ChatFormatting.GRAY));
            }
            case "party.invite" -> {
                inviteId = str(m, "id");
                inviteFrom = str(m, "from");
                inviteAt = System.currentTimeMillis();
                chat(Component.literal(Lang.t("net.partyInvite").replace("{name}", inviteFrom) + "  ").withStyle(ChatFormatting.GRAY)
                        .append(button(Lang.t("net.btnJoin"), ".party accept", ChatFormatting.GREEN)));
            }
            case "party.warp" -> {
                String address = str(m, "address");
                chat(Component.literal(Lang.t("net.warping").replace("{name}", str(m, "from")).replace("{server}", address)).withStyle(ChatFormatting.GRAY));
                warpTo(address);
            }
            case "error" -> {
                String code = str(m, "code");
                if ("AUTH_FAILED".equals(code)) { GruzClient.LOG.warn("Gruz: serwer odrzucił logowanie"); scheduleRetry(); }
                else chat(Component.literal(Lang.t("net.err." + code).replace("{name}", str(m, "name"))).withStyle(ChatFormatting.RED));
            }
            default -> { }
        }
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    /** Party: przejście na serwer lidera */
    private static void warpTo(String address) {
        Minecraft mc = Minecraft.getInstance();
        ServerData current = mc.getCurrentServer();
        if (current != null && current.ip.equalsIgnoreCase(address)) return;
        if (!ServerAddress.isValidAddress(address)) return;
        Compat.leaveWorld(mc);
        Compat.connect(new TitleScreen(), mc, address, Compat.server(address, address));
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
                    Minecraft.getInstance().execute(() -> {
                        try { handle(m); } catch (Exception e) { GruzClient.LOG.debug("Gruz: błąd wiadomości", e); }
                    });
                } catch (Exception ignored) { }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            Minecraft.getInstance().execute(GruzNetwork::scheduleRetry);
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            Minecraft.getInstance().execute(GruzNetwork::scheduleRetry);
        }
    }
}
